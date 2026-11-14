import dao.SummaryDAO;
import dao.TaxRuleDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Page;
import model.TaxParams;
import model.TaxRule;
import model.TaxRuleFilter;
import service.TaxToolsService;
import util.DBUtil;

public class TaxProfileCalcTest {

    private static int failures = 0;
    private static int passes = 0;
    private static final String TAG = "TPCT" + System.currentTimeMillis();
    private static final TaxRuleDAO RULES = new TaxRuleDAO();
    private static final TaxToolsService SVC = new TaxToolsService();
    private static final SummaryDAO SUMMARY = new SummaryDAO();
    private static final String[] SECTORS = {"GOODS", "SERVICE", "PRODUCTION", "OTHER"};

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (ok) {
            passes++;
        } else {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static String one(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void run(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    private static int count(String q, Object... args) throws Exception {
        return Integer.parseInt(one(q, args));
    }

    private static String num(Object v) {
        return v == null ? "null" : new BigDecimal(String.valueOf(v)).stripTrailingZeros().toPlainString();
    }

    private static Date d(String s) {
        return Date.valueOf(s);
    }

    private static int addRule(String kind, String sector, String value, String from, String to, String label, boolean verified) throws Exception {
        TaxRule r = new TaxRule();
        r.setKind(kind);
        r.setSector(sector);
        r.setValue(new BigDecimal(value));
        r.setEffectiveFrom(d(from));
        r.setEffectiveTo(to == null ? null : d(to));
        r.setSourceRef(TAG + "-" + label);
        r.setVerified(verified);
        r.setActive(true);
        RULES.insert(r);
        return Integer.parseInt(one("SELECT rule_id FROM TaxRules WHERE source_ref = ?", TAG + "-" + label));
    }

    private static boolean fails(String sqlText, Object... args) {
        try {
            run(sqlText, args);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private static Map<String, String> profile(String name, String type, String address, String branches, String channels,
            String registration, String tin, String method, String einvoice) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("type", type);
        m.put("address", address);
        m.put("branches", branches);
        m.put("channels", channels);
        m.put("registration", registration);
        m.put("tin", tin);
        m.put("method", method);
        m.put("einvoice", einvoice);
        return m;
    }

    private static Map<String, String> goodProfile() {
        return profile("Tinky Test", "HOUSEHOLD", "District 1, HCM", "3", "BOTH", "REGISTERED", "0312345678", "QUARTERLY", "NO");
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private static Map<String, String> with(Map<String, String> base, String key, String value) {
        Map<String, String> m = new LinkedHashMap<>(base);
        m.put(key, value);
        return m;
    }

    private static void validation() {
        Map<String, String> ok = goodProfile();
        check("validate: good profile has no errors", SVC.validate(ok).size(), 0);

        check("validate name empty", SVC.validate(with(ok, "name", "")).containsKey("name"), true);
        check("validate name 1 char", SVC.validate(with(ok, "name", "a")).containsKey("name"), true);
        check("validate name 2 chars", SVC.validate(with(ok, "name", "ab")).containsKey("name"), false);
        check("validate name 100 chars", SVC.validate(with(ok, "name", repeat('a', 100))).containsKey("name"), false);
        check("validate name 101 chars", SVC.validate(with(ok, "name", repeat('a', 101))).containsKey("name"), true);
        check("validate name UNKNOWN accepted", SVC.validate(with(ok, "name", "UNKNOWN")).containsKey("name"), false);

        check("validate address empty ok", SVC.validate(with(ok, "address", "")).containsKey("address"), false);
        check("validate address UNKNOWN ok", SVC.validate(with(ok, "address", "UNKNOWN")).containsKey("address"), false);
        check("validate address 4 chars", SVC.validate(with(ok, "address", "abcd")).containsKey("address"), true);
        check("validate address 5 chars", SVC.validate(with(ok, "address", "abcde")).containsKey("address"), false);
        check("validate address 100 chars", SVC.validate(with(ok, "address", repeat('x', 100))).containsKey("address"), false);
        check("validate address 101 chars", SVC.validate(with(ok, "address", repeat('x', 101))).containsKey("address"), true);

        String[] goodBranches = {"", "UNKNOWN", "1", "9", "10", "49", "50"};
        for (String b : goodBranches) {
            check("validate branches '" + b + "' ok", SVC.validate(with(ok, "branches", b)).containsKey("branches"), false);
        }
        String[] badBranches = {"0", "51", "99", "100", "01", "5a", "-1", "1.5", " 5"};
        for (String b : badBranches) {
            check("validate branches '" + b + "' rejected", SVC.validate(with(ok, "branches", b)).containsKey("branches"), true);
        }

        String[] goodTin = {"", "UNKNOWN", "1234567890", "1234567890-001"};
        for (String t : goodTin) {
            check("validate tin '" + t + "' ok", SVC.validate(with(ok, "tin", t)).containsKey("tin"), false);
        }
        String[] badTin = {"123456789", "12345678901", "1234567890-01", "1234567890-0011", "123456789a", "1234567890001", "abcdefghij", "1234567890-"};
        for (String t : badTin) {
            check("validate tin '" + t + "' rejected", SVC.validate(with(ok, "tin", t)).containsKey("tin"), true);
        }

        String[][] enums = {
            {"type", "HOUSEHOLD", "COMPANY", "INDIVIDUAL", "UNREGISTERED"},
            {"channels", "STORE", "ONLINE", "BOTH"},
            {"registration", "REGISTERED", "IN_PROGRESS", "NONE"},
            {"method", "QUARTERLY", "MONTHLY", "OTHER"},
            {"einvoice", "YES", "NO"}
        };
        for (String[] e : enums) {
            String key = e[0];
            for (int i = 1; i < e.length; i++) {
                check("validate " + key + " " + e[i] + " ok", SVC.validate(with(ok, key, e[i])).containsKey(key), false);
            }
            check("validate " + key + " empty ok", SVC.validate(with(ok, key, "")).containsKey(key), false);
            check("validate " + key + " UNKNOWN ok", SVC.validate(with(ok, key, "UNKNOWN")).containsKey(key), false);
            check("validate " + key + " lowercase rejected", SVC.validate(with(ok, key, e[1].toLowerCase())).containsKey(key), true);
            check("validate " + key + " bad value rejected", SVC.validate(with(ok, key, "NOPE")).containsKey(key), true);
        }

        Map<String, String> allBad = profile("", "x", "abc", "0", "x", "x", "1", "x", "x");
        check("validate all bad reports 9 errors", SVC.validate(allBad).size(), 9);
        Map<String, String> allUnknown = profile("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN");
        check("validate all UNKNOWN has no errors", SVC.validate(allUnknown).size(), 0);
        Map<String, String> allEmptyButName = profile("Ab", "", "", "", "", "", "", "", "");
        check("validate all empty but name has no errors", SVC.validate(allEmptyButName).size(), 0);
    }

    private static void completion() {
        check("completion all filled", SVC.completion(goodProfile()), 100);
        check("completion all empty", SVC.completion(profile("", "", "", "", "", "", "", "", "")), 0);
        check("completion all UNKNOWN is half", SVC.completion(profile("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN")), 50);
        check("completion one field", SVC.completion(profile("Ab", "", "", "", "", "", "", "", "")), 11);
        check("completion one UNKNOWN", SVC.completion(profile("UNKNOWN", "", "", "", "", "", "", "", "")), 6);
        check("completion 7 of 9", SVC.completion(profile("Ab", "HOUSEHOLD", "District 1", "2", "BOTH", "REGISTERED", "0312345678", "", "")), 78);
        check("completion mixed 4 filled 2 unknown", SVC.completion(profile("Ab", "HOUSEHOLD", "UNKNOWN", "2", "UNKNOWN", "", "", "", "YES")), 56);
        Map<String, String> partial = new LinkedHashMap<>();
        partial.put("name", "Ab");
        check("completion missing keys count as empty", SVC.completion(partial), 11);
        Map<String, String> extra = goodProfile();
        extra.put("sector", "GOODS");
        check("completion ignores sector key", SVC.completion(extra), 100);
    }

    private static Map<String, Map<String, String>> snapshotSettings() throws Exception {
        Map<String, Map<String, String>> snap = new LinkedHashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT setting_key, store_id, setting_value FROM Settings "
                     + "WHERE setting_key LIKE N'taxp[_]%' OR setting_key = N'tax_sector'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("key", rs.getString(1));
                row.put("store", rs.getString(2));
                row.put("value", rs.getString(3));
                snap.put(rs.getString(1) + "|" + rs.getString(2), row);
            }
        }
        return snap;
    }

    private static void restoreSettings(Map<String, Map<String, String>> snap) throws Exception {
        List<String[]> current = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT setting_key, store_id FROM Settings "
                     + "WHERE setting_key LIKE N'taxp[_]%' OR setting_key = N'tax_sector'");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                current.add(new String[]{rs.getString(1), rs.getString(2)});
            }
        }
        for (String[] c : current) {
            if (!snap.containsKey(c[0] + "|" + c[1])) {
                run("DELETE FROM Settings WHERE setting_key = ? AND store_id = ?", c[0], Integer.parseInt(c[1]));
            }
        }
        for (Map<String, String> row : snap.values()) {
            int n = count("SELECT COUNT(*) FROM Settings WHERE setting_key = ? AND store_id = ?", row.get("key"), Integer.parseInt(row.get("store")));
            if (n == 0) {
                run("INSERT INTO Settings (setting_key, setting_value, store_id) VALUES (?, ?, ?)", row.get("key"), row.get("value"), Integer.parseInt(row.get("store")));
            } else {
                run("UPDATE Settings SET setting_value = ? WHERE setting_key = ? AND store_id = ?", row.get("value"), row.get("key"), Integer.parseInt(row.get("store")));
            }
        }
    }

    private static void profileStorage() throws Exception {
        Map<String, String> in = goodProfile();
        in.put("sector", "SERVICE");
        SVC.saveProfile(in);
        Map<String, String> back = SVC.profile();
        check("profile roundtrip name", back.get("name"), "Tinky Test");
        check("profile roundtrip type", back.get("type"), "HOUSEHOLD");
        check("profile roundtrip branches", back.get("branches"), "3");
        check("profile roundtrip tin", back.get("tin"), "0312345678");
        check("profile roundtrip einvoice", back.get("einvoice"), "NO");
        check("profile sector saved", back.get("sector"), "SERVICE");
        check("profile has 10 keys", back.size(), 10);

        Map<String, String> bad = goodProfile();
        bad.put("sector", "HACK");
        bad.put("name", "Second Name");
        SVC.saveProfile(bad);
        Map<String, String> after = SVC.profile();
        check("saveProfile invalid sector keeps old sector", after.get("sector"), "SERVICE");
        check("saveProfile still saves fields", after.get("name"), "Second Name");

        Map<String, String> noSector = goodProfile();
        SVC.saveProfile(noSector);
        check("saveProfile without sector keeps sector", SVC.profile().get("sector"), "SERVICE");

        for (String s : SECTORS) {
            Map<String, String> m = goodProfile();
            m.put("sector", s);
            SVC.saveProfile(m);
            check("saveProfile sector " + s, SVC.profile().get("sector"), s);
        }
        Map<String, String> lower = goodProfile();
        lower.put("sector", "goods");
        SVC.saveProfile(lower);
        check("saveProfile lowercase sector rejected", SVC.profile().get("sector"), "OTHER");

        Map<String, String> unknown = profile("UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN");
        SVC.saveProfile(unknown);
        check("UNKNOWN is stored and counted half", SVC.completion(SVC.profile()), 50);
    }

    private static BigDecimal vatOf(BigDecimal rev, TaxParams p) {
        if (rev.compareTo(p.getThreshold()) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal keep = BigDecimal.ONE.subtract(p.getVatReductionPct().divide(new BigDecimal("100")));
        return rev.multiply(p.getVatRatePct()).divide(new BigDecimal("100")).multiply(keep).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal pitOf(BigDecimal rev, TaxParams p) {
        if (rev.compareTo(p.getThreshold()) <= 0 || rev.compareTo(p.getPitCeiling()) > 0) {
            return BigDecimal.ZERO;
        }
        return rev.subtract(p.getThreshold()).multiply(p.getPitRatePct()).divide(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP);
    }

    private static Map<String, Object> calc(String sector, String from, String to, String rev, String adj) throws Exception {
        return SVC.calculate(sector, d(from), d(to), new BigDecimal(rev), new BigDecimal(adj));
    }

    private static boolean calcThrows(String sector, String from, String to, String rev, String adj) throws Exception {
        try {
            calc(sector, from, to, rev, adj);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static void calculations() throws Exception {
        check("calc year 2025 by sector: no rules before setup", calcThrows("GOODS", "2025-01-01", "2025-12-31", "1", "0"), true);

        String[] sec = SECTORS;
        String[] vat = {"1", "5", "3", "2"};
        String[] pit = {"0.5", "2", "1.5", "1"};
        addRule("THRESHOLD", null, "1000000000", "2025-01-01", "2025-12-31", "thr", true);
        addRule("PIT_CEILING", null, "3000000000", "2025-01-01", "2025-12-31", "ceil", true);
        addRule("WARN_LEVEL", null, "70", "2025-01-01", "2025-12-31", "w70", true);
        addRule("WARN_LEVEL", null, "90", "2025-01-01", "2025-12-31", "w90", true);
        int w100 = addRule("WARN_LEVEL", null, "100", "2025-01-01", "2025-12-31", "w100", true);
        int[] vatId = new int[4];
        int[] pitId = new int[4];
        for (int i = 0; i < 4; i++) {
            vatId[i] = addRule("VAT_RATE", sec[i], vat[i], "2025-01-01", "2025-12-31", "vat" + sec[i], true);
            pitId[i] = addRule("PIT_RATE", sec[i], pit[i], "2025-01-01", "2025-12-31", "pit" + sec[i], true);
        }

        check("inForce GOODS mid 2025 count", RULES.inForce("GOODS", d("2025-06-15")).size(), 7);
        check("inForce SERVICE mid 2025 count", RULES.inForce("SERVICE", d("2025-06-15")).size(), 7);
        check("inForce first day included", RULES.inForce("GOODS", d("2025-01-01")).size(), 7);
        check("inForce last day included", RULES.inForce("GOODS", d("2025-12-31")).size(), 7);

        TaxParams tp = RULES.loadParams("GOODS", d("2025-06-15"));
        check("loadParams GOODS threshold", num(tp.getThreshold()), "1000000000");
        check("loadParams GOODS ceiling", num(tp.getPitCeiling()), "3000000000");
        check("loadParams GOODS vat", num(tp.getVatRatePct()), "1");
        check("loadParams GOODS pit", num(tp.getPitRatePct()), "0.5");
        check("loadParams reduction default zero", num(tp.getVatReductionPct()), "0");
        check("loadParams warn levels", num(tp.getWarn1()) + "/" + num(tp.getWarn2()) + "/" + num(tp.getWarn3()), "70/90/100");
        check("loadParams unknown sector is null", RULES.loadParams("NOSUCH", d("2025-06-15")) == null, true);

        String[] ratesVat = {"1", "5", "3", "2"};
        String[] ratesPit = {"0.5", "2", "1.5", "1"};
        long[] expVat2 = {20000000L, 100000000L, 60000000L, 40000000L};
        long[] expPit2 = {5000000L, 20000000L, 15000000L, 10000000L};
        for (int i = 0; i < 4; i++) {
            Map<String, Object> m = calc(sec[i], "2025-01-01", "2025-12-31", "2000000000", "0");
            check("calc " + sec[i] + " 2bn vat rate", num(m.get("vatRate")), ratesVat[i]);
            check("calc " + sec[i] + " 2bn pit rate", num(m.get("pitRate")), ratesPit[i]);
            check("calc " + sec[i] + " 2bn vat", num(m.get("vat")), expVat2[i]);
            check("calc " + sec[i] + " 2bn pit", num(m.get("pit")), expPit2[i]);
            check("calc " + sec[i] + " 2bn status", m.get("status"), "OVER");
        }

        Map<String, Object> m = calc("GOODS", "2025-01-01", "2025-12-31", "1000000000", "0");
        check("calc exactly threshold vat zero", num(m.get("vat")), "0");
        check("calc exactly threshold pit zero", num(m.get("pit")), "0");
        check("calc exactly threshold status UNDER", m.get("status"), "UNDER");
        check("calc cumulative and taxable", num(m.get("cumulative")) + "/" + num(m.get("taxable")) + "/" + num(m.get("prior")), "1000000000/1000000000/0");

        m = calc("GOODS", "2025-01-01", "2025-12-31", "1000001000", "0");
        check("calc just above threshold status", m.get("status"), "OVER");
        check("calc just above threshold vat", num(m.get("vat")), "10000010");
        check("calc just above threshold pit", num(m.get("pit")), "5");

        m = calc("GOODS", "2025-01-01", "2025-12-31", "999999999", "0");
        check("calc below threshold status", m.get("status"), "UNDER");
        check("calc below threshold vat", num(m.get("vat")), "0");

        m = calc("GOODS", "2025-01-01", "2025-12-31", "3000000000", "0");
        check("calc at ceiling status", m.get("status"), "OVER");
        check("calc at ceiling pit", num(m.get("pit")), "10000000");
        check("calc at ceiling vat", num(m.get("vat")), "30000000");
        check("calc at ceiling flag", m.get("ceiling"), false);

        m = calc("GOODS", "2025-01-01", "2025-12-31", "3000000001", "0");
        check("calc above ceiling status", m.get("status"), "OVER_CEILING");
        check("calc above ceiling flag", m.get("ceiling"), true);
        check("calc above ceiling pit zero", num(m.get("pit")), "0");
        check("calc above ceiling vat still computed", num(m.get("vat")), "30000000");

        m = calc("GOODS", "2025-01-01", "2025-12-31", "2000000000", "1000000000");
        check("calc adjustment reduces taxable", num(m.get("taxable")), "1000000000");
        check("calc adjustment to threshold gives no tax", num(m.get("vat")) + "/" + num(m.get("pit")), "0/0");
        m = calc("GOODS", "2025-01-01", "2025-12-31", "2000000000", "500000000");
        check("calc adjustment partial vat", num(m.get("vat")), "15000000");
        check("calc adjustment partial pit", num(m.get("pit")), "2500000");
        m = calc("GOODS", "2025-01-01", "2025-12-31", "500", "500");
        check("calc adjustment equal to revenue ok", num(m.get("taxable")), "0");
        check("calc adjustment above revenue rejected", calcThrows("GOODS", "2025-01-01", "2025-12-31", "500", "501"), true);
        check("calc adjustment far above revenue rejected", calcThrows("GOODS", "2025-01-01", "2025-12-31", "0", "1"), true);
        check("calc zero revenue", num(calc("GOODS", "2025-01-01", "2025-12-31", "0", "0").get("vat")), "0");
        check("calc unknown sector rejected", calcThrows("NOSUCH", "2025-01-01", "2025-12-31", "1", "0"), true);
        check("calc date without rules rejected", calcThrows("GOODS", "2023-01-01", "2023-12-31", "1", "0"), true);

        int red = addRule("VAT_REDUCTION", null, "20", "2025-07-01", "2025-12-31", "red", true);
        m = calc("GOODS", "2025-01-01", "2025-09-30", "2000000000", "0");
        check("calc VAT reduction applied", num(m.get("vat")), "16000000");
        check("calc VAT reduction leaves pit", num(m.get("pit")), "5000000");
        m = calc("GOODS", "2025-01-01", "2025-03-31", "2000000000", "0");
        check("calc VAT reduction not yet in force", num(m.get("vat")), "20000000");
        RULES.setActive(red, false);
        m = calc("GOODS", "2025-01-01", "2025-09-30", "2000000000", "0");
        check("calc disabled VAT reduction ignored", num(m.get("vat")), "20000000");
        RULES.setActive(red, true);

        BigDecimal prior = SUMMARY.revenueBetween(d("2025-01-01"), d("2025-06-30"));
        String[] taxables = {"500000000", "2000000000", "3500000000"};
        for (String t : taxables) {
            TaxParams p = RULES.loadParams("SERVICE", d("2025-09-30"));
            m = calc("SERVICE", "2025-07-01", "2025-09-30", t, "0");
            BigDecimal taxable = new BigDecimal(t);
            BigDecimal cum = prior.add(taxable);
            check("calc period prior from year start " + t, num(m.get("prior")), num(prior));
            check("calc period cumulative " + t, num(m.get("cumulative")), num(cum));
            BigDecimal expVat = vatOf(cum, p).subtract(vatOf(prior, p)).max(BigDecimal.ZERO);
            check("calc period vat = ytd tax minus tax before " + t, num(m.get("vat")), num(expVat));
            boolean ceil = cum.compareTo(p.getPitCeiling()) > 0;
            BigDecimal expPit = ceil ? BigDecimal.ZERO : pitOf(cum, p).subtract(pitOf(prior, p)).max(BigDecimal.ZERO);
            check("calc period pit = ytd tax minus tax before " + t, num(m.get("pit")), num(expPit));
        }

        Map<String, Object> stampBefore = calc("GOODS", "2025-01-01", "2025-12-31", "10", "0");
        String st0 = String.valueOf(stampBefore.get("stamp"));
        check("calc stamp lists the vat rule", st0.contains(vatId[0] + ":VAT_RATE=1@"), true);
        check("calc stamp not empty", st0.isEmpty(), false);
        check("stamp is stable", SVC.stamp("GOODS", LocalDate.of(2025, 6, 15)), SVC.stamp("GOODS", LocalDate.of(2025, 6, 15)));
        check("calc stamp equals service stamp", st0, SVC.stamp("GOODS", LocalDate.of(2025, 12, 31)));
        check("stamp differs per sector", SVC.stamp("GOODS", LocalDate.of(2025, 6, 15)).equals(SVC.stamp("SERVICE", LocalDate.of(2025, 6, 15))), false);
        check("stamp empty when no rules", SVC.stamp("GOODS", LocalDate.of(2023, 6, 15)), "");

        String s1 = SVC.stamp("GOODS", LocalDate.of(2025, 6, 15));
        TaxRule vr = RULES.findById(vatId[0]);
        vr.setValue(new BigDecimal("1.5"));
        check("update rule returns true", RULES.update(vr), true);
        String s2 = SVC.stamp("GOODS", LocalDate.of(2025, 6, 15));
        check("stamp changes when a rule value changes", s1.equals(s2), false);
        check("stamp shows new value", s2.contains(vatId[0] + ":VAT_RATE=1.5@"), true);
        m = calc("GOODS", "2025-01-01", "2025-06-30", "2000000000", "0");
        check("calc uses the updated vat rate", num(m.get("vat")), "30000000");
        check("calc carries the updated stamp", m.get("stamp"), SVC.stamp("GOODS", LocalDate.of(2025, 6, 30)));

        check("rulesVerified true when all verified", SVC.rulesVerified("GOODS", LocalDate.of(2025, 6, 15)), true);
        check("calc verified true", calc("GOODS", "2025-01-01", "2025-12-31", "10", "0").get("verified"), true);
        vr = RULES.findById(vatId[0]);
        vr.setVerified(false);
        RULES.update(vr);
        check("rulesVerified false when a rule is not verified", SVC.rulesVerified("GOODS", LocalDate.of(2025, 6, 15)), false);
        check("calc verified false", calc("GOODS", "2025-01-01", "2025-12-31", "10", "0").get("verified"), false);
        check("rulesVerified other sector unaffected", SVC.rulesVerified("SERVICE", LocalDate.of(2025, 6, 15)), true);
        check("stamp marks unverified rule", SVC.stamp("GOODS", LocalDate.of(2025, 6, 15)).contains(vatId[0] + ":VAT_RATE=1.5@") && SVC.stamp("GOODS", LocalDate.of(2025, 6, 15)).contains("-"), true);
        check("stamp changes when verified flips", s2.equals(SVC.stamp("GOODS", LocalDate.of(2025, 6, 15))), false);
        check("rulesVerified false when no rules", SVC.rulesVerified("GOODS", LocalDate.of(2023, 6, 15)), false);
        vr.setVerified(true);
        vr.setValue(new BigDecimal("1"));
        RULES.update(vr);

        RULES.setActive(w100, false);
        check("loadParams null when a warn level is disabled", RULES.loadParams("GOODS", d("2025-06-15")) == null, true);
        check("calc rejected when a warn level is disabled", calcThrows("GOODS", "2025-01-01", "2025-12-31", "10", "0"), true);
        RULES.setActive(w100, true);
        check("loadParams back after re-enable", RULES.loadParams("GOODS", d("2025-06-15")) != null, true);

        int thrNew = addRule("THRESHOLD", null, "2000000000", "2025-07-01", "2025-12-31", "thrnew", true);
        check("loadParams newer rule wins after its start", num(RULES.loadParams("GOODS", d("2025-09-01")).getThreshold()), "2000000000");
        check("loadParams newer rule not yet in force", num(RULES.loadParams("GOODS", d("2025-03-01")).getThreshold()), "1000000000");
        m = calc("GOODS", "2025-01-01", "2025-09-30", "1500000000", "0");
        check("calc uses threshold at period end (under new threshold)", m.get("status"), "UNDER");
        RULES.setActive(thrNew, false);
        check("loadParams ignores disabled rule", num(RULES.loadParams("GOODS", d("2025-09-01")).getThreshold()), "1000000000");
        RULES.setActive(thrNew, true);
        check("loadParams re-enabled rule back", num(RULES.loadParams("GOODS", d("2025-09-01")).getThreshold()), "2000000000");
        check("delete rule returns true", RULES.delete(thrNew), true);
        check("loadParams after delete", num(RULES.loadParams("GOODS", d("2025-09-01")).getThreshold()), "1000000000");

        addRule("THRESHOLD", null, "5000000000", "2025-01-01", "2025-02-28", "throld", true);
        check("loadParams expired rule ignored after its end", num(RULES.loadParams("GOODS", d("2025-03-01")).getThreshold()), "1000000000");
        check("loadParams expired rule used inside window", num(RULES.loadParams("GOODS", d("2025-02-28")).getThreshold()), "5000000000");
        addRule("THRESHOLD", null, "777", "2026-01-01", "2026-01-02", "thrfut", true);
        check("loadParams future rule ignored", num(RULES.loadParams("GOODS", d("2025-12-31")).getThreshold()), "1000000000");
        TaxRuleFilter all = new TaxRuleFilter();
        all.setQ(TAG);
        check("search all tagged rows paged to 10", RULES.search(all).getItems().size(), 10);
        check("search all tagged rows total more than a page", RULES.search(all).getTotal() > 10, true);
        all.setPage(2);
        check("search page 2 has rows", RULES.search(all).getItems().size() > 0, true);
    }

    private static void ruleAdmin() throws Exception {
        int id = addRule("VAT_RATE", "SERVICE", "7.25", "2024-01-01", "2024-06-30", "adm", false);
        TaxRule r = RULES.findById(id);
        check("findById kind", r.getKind(), "VAT_RATE");
        check("findById sector", r.getSector(), "SERVICE");
        check("findById value", num(r.getValue()), "7.25");
        check("findById dates", r.getEffectiveFrom() + "/" + r.getEffectiveTo(), "2024-01-01/2024-06-30");
        check("findById verified false", r.isVerified(), false);
        check("findById active default true", r.isActive(), true);
        check("findById source ref", r.getSourceRef(), TAG + "-adm");
        check("findById missing is null", RULES.findById(-5) == null, true);

        java.sql.Timestamp t0 = r.getUpdatedAt();
        Thread.sleep(30);
        r.setValue(new BigDecimal("8.5"));
        r.setSector(null);
        r.setKind("THRESHOLD");
        r.setEffectiveTo(null);
        r.setVerified(true);
        r.setSourceRef(TAG + "-adm2");
        check("update returns true", RULES.update(r), true);
        TaxRule u = RULES.findById(id);
        check("update changed value", num(u.getValue()), "8.5");
        check("update changed kind and sector", u.getKind() + "/" + u.getSector(), "THRESHOLD/null");
        check("update cleared effective_to", u.getEffectiveTo() == null, true);
        check("update verified", u.isVerified(), true);
        check("update source ref", u.getSourceRef(), TAG + "-adm2");
        check("update moves updated_at forward", u.getUpdatedAt().after(t0), true);
        check("update does not change active", u.isActive(), true);
        TaxRule ghost = RULES.findById(id);
        ghost.setRuleId(-77);
        check("update unknown id returns false", RULES.update(ghost), false);

        check("setActive false returns true", RULES.setActive(id, false), true);
        check("setActive false persisted", RULES.findById(id).isActive(), false);
        check("disabled rule not in inForce", RULES.inForce("GOODS", d("2024-03-01")).size(), 0);
        check("setActive true returns true", RULES.setActive(id, true), true);
        check("enabled rule in inForce", RULES.inForce("GOODS", d("2024-03-01")).size(), 1);
        check("setActive unknown id returns false", RULES.setActive(-77, true), false);

        TaxRuleFilter f = new TaxRuleFilter();
        f.setQ(TAG + "-adm2");
        Page<TaxRule> pg = RULES.search(f);
        check("search by source ref total", pg.getTotal(), 1);
        check("search returns the row", pg.getItems().get(0).getRuleId(), id);
        f.setKind("VAT_RATE");
        check("search kind filter excludes", RULES.search(f).getTotal(), 0);
        f.setKind("THRESHOLD");
        check("search kind filter includes", RULES.search(f).getTotal(), 1);
        TaxRuleFilter seedF = new TaxRuleFilter();
        seedF.setKind("PIT_CEILING");
        check("search by kind finds seeded ceiling", RULES.search(seedF).getTotal() >= 1, true);
        TaxRuleFilter none = new TaxRuleFilter();
        none.setQ(TAG + "-does-not-exist");
        check("search no match", RULES.search(none).getTotal(), 0);

        String base = "INSERT INTO TaxRules (kind, sector, value, effective_from, effective_to, source_ref, verified) VALUES (?, ?, ?, ?, ?, ?, 1)";
        check("db rejects unknown kind", fails(base, "BOGUS", null, 1, "2024-01-01", null, TAG + "-c1"), true);
        check("db rejects unknown sector", fails(base, "VAT_RATE", "SPACE", 1, "2024-01-01", null, TAG + "-c2"), true);
        check("db rejects negative value", fails(base, "VAT_RATE", "GOODS", -1, "2024-01-01", null, TAG + "-c3"), true);
        check("db rejects end before start", fails(base, "VAT_RATE", "GOODS", 1, "2024-02-01", "2024-01-01", TAG + "-c4"), true);
        check("db rejects missing source ref", fails(base, "VAT_RATE", "GOODS", 1, "2024-01-01", null, null), true);
        check("db accepts zero value", fails(base, "VAT_RATE", "GOODS", 0, "2024-01-01", "2024-01-02", TAG + "-c5"), false);
        check("db accepts same start and end", fails(base, "VAT_RATE", "GOODS", 1, "2024-01-01", "2024-01-01", TAG + "-c6"), false);
        check("rejected rows left nothing", count("SELECT COUNT(*) FROM TaxRules WHERE source_ref IN (?, ?, ?, ?)", TAG + "-c1", TAG + "-c2", TAG + "-c3", TAG + "-c4"), 0);

        check("delete returns true", RULES.delete(id), true);
        check("deleted rule is gone", RULES.findById(id) == null, true);
        check("delete twice returns false", RULES.delete(id), false);
    }

    private static Map<String, Object> kind(List<Map<String, Object>> rows, String k) {
        for (Map<String, Object> r : rows) {
            if (k.equals(r.get("kind"))) {
                return r;
            }
        }
        return null;
    }

    private static String expectTax(String type, boolean over, boolean near, boolean solid, boolean pit) {
        boolean typeKnown = !type.isEmpty() && !"UNKNOWN".equals(type) && !"UNREGISTERED".equals(type);
        if (!typeKnown) {
            return "NEEDS_VERIFICATION";
        }
        if ("COMPANY".equals(type)) {
            return pit ? (solid ? "NOT_APPLICABLE" : "NEEDS_VERIFICATION") : "APPLICABLE";
        }
        if (over) {
            return solid ? "APPLICABLE" : "NEEDS_VERIFICATION";
        }
        return (near || !solid) ? "NEEDS_VERIFICATION" : "NOT_APPLICABLE";
    }

    private static void verifyCheck(String label, Map<String, String> p, String sector) throws Exception {
        LocalDate today = LocalDate.now();
        TaxParams params = RULES.loadParams(sector, Date.valueOf(today));
        BigDecimal revenue = SUMMARY.yearRevenue(today.getYear());
        boolean over = revenue.compareTo(params.getThreshold()) > 0;
        boolean near = revenue.multiply(new BigDecimal("100")).compareTo(params.getThreshold().multiply(new BigDecimal("70"))) >= 0;
        int pc = SVC.completion(p);
        String type = p.get("type");
        boolean typeKnown = !type.isEmpty() && !"UNKNOWN".equals(type) && !"UNREGISTERED".equals(type);
        boolean solid = typeKnown && pc >= 70;
        Map<String, String> in = new LinkedHashMap<>(p);
        in.put("sector", sector);
        List<Map<String, Object>> rows = SVC.check(in);
        check(label + " has 4 rows", rows.size(), 4);
        check(label + " VAT", kind(rows, "VAT").get("status"), expectTax(type, over, near, solid, false));
        check(label + " PIT", kind(rows, "PIT").get("status"), expectTax(type, over, near, solid, true));
        boolean eInv = "COMPANY".equals(type) || over;
        check(label + " EINVOICE", kind(rows, "EINVOICE").get("status"), eInv ? "APPLICABLE" : "NEEDS_VERIFICATION");
        check(label + " BOOKKEEPING", kind(rows, "BOOKKEEPING").get("status"), "APPLICABLE");
        boolean rv = SVC.rulesVerified(sector, today);
        for (Map<String, Object> r : rows) {
            check(label + " " + r.get("kind") + " verified flag", r.get("verified"), rv);
            check(label + " " + r.get("kind") + " status allowed",
                    "APPLICABLE|NOT_APPLICABLE|NEEDS_VERIFICATION".contains(String.valueOf(r.get("status"))), true);
        }
        String basis = String.valueOf(kind(rows, "VAT").get("basis"));
        check(label + " basis mentions verification", basis.contains(rv ? "all verified" : "not all verified"), true);
    }

    @SuppressWarnings("unchecked")
    private static void checker() throws Exception {
        LocalDate today = LocalDate.now();
        verifyCheck("check HOUSEHOLD complete", goodProfile(), "GOODS");
        verifyCheck("check HOUSEHOLD complete SERVICE", goodProfile(), "SERVICE");
        verifyCheck("check INDIVIDUAL complete", with(goodProfile(), "type", "INDIVIDUAL"), "PRODUCTION");
        verifyCheck("check COMPANY complete", with(goodProfile(), "type", "COMPANY"), "GOODS");
        verifyCheck("check type UNKNOWN", with(goodProfile(), "type", "UNKNOWN"), "GOODS");
        verifyCheck("check type empty", with(goodProfile(), "type", ""), "GOODS");
        verifyCheck("check UNREGISTERED", with(goodProfile(), "type", "UNREGISTERED"), "GOODS");
        verifyCheck("check HOUSEHOLD thin profile", profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", ""), "GOODS");
        verifyCheck("check COMPANY thin profile", profile("Ab", "COMPANY", "", "", "", "", "", "", ""), "GOODS");
        verifyCheck("check everything empty", profile("", "", "", "", "", "", "", "", ""), "GOODS");

        Map<String, String> unknownType = with(goodProfile(), "type", "UNKNOWN");
        List<Map<String, Object>> rows = SVC.check(with(unknownType, "sector", "GOODS"));
        check("unknown type VAT never decided", kind(rows, "VAT").get("status"), "NEEDS_VERIFICATION");
        check("unknown type PIT never decided", kind(rows, "PIT").get("status"), "NEEDS_VERIFICATION");
        check("unknown type lists missing type (VAT)", ((List<String>) kind(rows, "VAT").get("missing")).contains("Business type"), true);
        check("unknown type lists missing type (PIT)", ((List<String>) kind(rows, "PIT").get("missing")).contains("Business type"), true);
        check("unknown type lists missing type (EINVOICE)", ((List<String>) kind(rows, "EINVOICE").get("missing")).contains("Business type"), true);
        check("complete profile VAT missing nothing", ((List<String>) kind(SVC.check(with(goodProfile(), "sector", "GOODS")), "VAT").get("missing")).size(), 0);
        List<String> missVat = (List<String>) kind(SVC.check(with(profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", ""), "sector", "GOODS")), "VAT").get("missing");
        check("empty details listed for VAT", missVat.toString(), "[Registration status, Declaration method]");
        List<String> missPit = (List<String>) kind(SVC.check(with(profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", ""), "sector", "GOODS")), "PIT").get("missing");
        check("empty details listed for PIT", missPit.toString(), "[Registration status]");
        List<String> missEinv = (List<String>) kind(SVC.check(with(profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", "UNKNOWN"), "sector", "GOODS")), "EINVOICE").get("missing");
        check("UNKNOWN detail listed for EINVOICE", missEinv.toString(), "[E-invoice registration]");
        check("bookkeeping lists nothing missing", ((List<String>) kind(rows, "BOOKKEEPING").get("missing")).size(), 0);
        for (Map<String, Object> r : rows) {
            check("row " + r.get("kind") + " has reason", String.valueOf(r.get("reason")).isEmpty(), false);
            check("row " + r.get("kind") + " has basis", String.valueOf(r.get("basis")).isEmpty(), false);
        }

        List<Object[]> condsVat = (List<Object[]>) kind(rows, "VAT").get("conds");
        boolean hasType = false;
        for (Object[] c : condsVat) {
            if (String.valueOf(c[0]).equals("Business type is known")) {
                hasType = true;
                check("conds mark unknown type as undecided", c[1], null);
            }
        }
        check("conds include the type line", hasType, true);

        check("check without sector key uses saved sector", SVC.check(goodProfile()).size(), 4);

        BigDecimal revenue = SUMMARY.yearRevenue(today.getYear());
        Map<String, String> small = with(goodProfile(), "sector", "GOODS");
        if (revenue.signum() > 0) {
            BigDecimal tUnder = revenue.divide(new BigDecimal("2"), 0, RoundingMode.DOWN).max(BigDecimal.ONE);
            int a = addRule("THRESHOLD", null, tUnder.toPlainString(), today.toString(), today.toString(), "chk-a", true);
            verifyCheck("check revenue over threshold HOUSEHOLD", small, "GOODS");
            verifyCheck("check revenue over threshold COMPANY", with(small, "type", "COMPANY"), "GOODS");
            verifyCheck("check revenue over threshold thin profile", profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", ""), "GOODS");
            Map<String, Object> over = kind(SVC.check(small), "VAT");
            check("over threshold small complete VAT is APPLICABLE", over.get("status"), "APPLICABLE");
            check("over threshold EINVOICE applicable", kind(SVC.check(small), "EINVOICE").get("status"), "APPLICABLE");
            RULES.delete(a);
            BigDecimal tNear = revenue.multiply(new BigDecimal("1.2")).setScale(0, RoundingMode.UP);
            int b = addRule("THRESHOLD", null, tNear.toPlainString(), today.toString(), today.toString(), "chk-b", true);
            verifyCheck("check revenue near threshold", small, "GOODS");
            check("near threshold is NEEDS_VERIFICATION", kind(SVC.check(small), "VAT").get("status"), "NEEDS_VERIFICATION");
            RULES.delete(b);
            BigDecimal tFar = revenue.multiply(new BigDecimal("10"));
            int c = addRule("THRESHOLD", null, tFar.toPlainString(), today.toString(), today.toString(), "chk-c", true);
            verifyCheck("check revenue far below threshold", small, "GOODS");
            check("far below threshold complete household NOT_APPLICABLE", kind(SVC.check(small), "VAT").get("status"), "NOT_APPLICABLE");
            check("far below threshold thin profile stays undecided", kind(SVC.check(profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", "")), "VAT").get("status"), "NEEDS_VERIFICATION");
            RULES.delete(c);
        } else {
            System.out.println("NOTE revenue this year is 0: over and near branches of check() not driven");
            int c = addRule("THRESHOLD", null, "1000000", today.toString(), today.toString(), "chk-c", true);
            check("zero revenue complete household NOT_APPLICABLE", kind(SVC.check(small), "VAT").get("status"), "NOT_APPLICABLE");
            check("zero revenue thin profile stays undecided", kind(SVC.check(profile("Ab", "HOUSEHOLD", "", "", "", "", "", "", "")), "VAT").get("status"), "NEEDS_VERIFICATION");
            RULES.delete(c);
        }

        int unv = addRule("VAT_RATE", "GOODS", "9", today.toString(), today.toString(), "chk-unv", false);
        List<Map<String, Object>> r1 = SVC.check(with(goodProfile(), "sector", "GOODS"));
        check("an unverified rule in force makes check report not verified", kind(r1, "VAT").get("verified"), false);
        check("basis says not all verified", String.valueOf(kind(r1, "VAT").get("basis")).contains("not all verified"), true);
        RULES.delete(unv);
    }

    public static void main(String[] args) throws Exception {
        Map<String, Map<String, String>> snap = snapshotSettings();
        try {
            check("precondition no rules in 2025 before the test", count("SELECT COUNT(*) FROM TaxRules WHERE effective_from <= '2025-12-31' AND (effective_to IS NULL OR effective_to >= '2025-01-01') AND source_ref NOT LIKE ?", TAG + "%"), 0);
            validation();
            completion();
            profileStorage();
            ruleAdmin();
            calculations();
            checker();
        } catch (Throwable t) {
            failures++;
            System.out.println("FAIL unexpected " + t);
            t.printStackTrace(System.out);
        } finally {
            try {
                run("DELETE FROM TaxRules WHERE source_ref LIKE ?", TAG + "%");
                restoreSettings(snap);
                check("cleanup tagged rules", count("SELECT COUNT(*) FROM TaxRules WHERE source_ref LIKE ?", TAG + "%"), 0);
                check("cleanup settings restored", snapshotSettings().toString(), snap.toString());
            } catch (Throwable t) {
                failures++;
                System.out.println("FAIL cleanup " + t);
            }
        }
        System.out.println(failures == 0 ? "ALL OK (" + passes + " checks)" : failures + " FAILED (" + passes + " ok)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
