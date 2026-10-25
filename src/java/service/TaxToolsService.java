package service;

import dao.SettingDAO;
import dao.SummaryDAO;
import dao.TaxRuleDAO;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.TaxParams;
import model.TaxRule;
import model.view.TaxEstimate;

public class TaxToolsService {

    public static final String UNKNOWN = "UNKNOWN";
    public static final String[] FIELDS = {"name", "type", "address", "branches", "channels", "registration", "tin", "method", "einvoice"};
    public static final Map<String, String> LABELS = new LinkedHashMap<>();

    static {
        LABELS.put("name", "Business name");
        LABELS.put("type", "Business type");
        LABELS.put("address", "Address");
        LABELS.put("branches", "Number of branches");
        LABELS.put("channels", "Sales channels");
        LABELS.put("registration", "Registration status");
        LABELS.put("tin", "Tax identification number");
        LABELS.put("method", "Declaration method");
        LABELS.put("einvoice", "E-invoice registration");
    }

    private final SettingDAO settingDAO = new SettingDAO();
    private final SummaryDAO summaryDAO = new SummaryDAO();
    private final TaxRuleDAO taxRuleDAO = new TaxRuleDAO();

    public Map<String, String> profile() throws SQLException {
        Map<String, String> p = new LinkedHashMap<>();
        for (String f : FIELDS) {
            p.put(f, settingDAO.get("taxp_" + f, ""));
        }
        p.put("sector", settingDAO.get("tax_sector", "GOODS"));
        return p;
    }

    public Map<String, String> validate(Map<String, String> in) {
        Map<String, String> errors = new LinkedHashMap<>();
        String name = in.get("name");
        if (name.length() < 2 || name.length() > 100) {
            errors.put("name", "2-100 characters.");
        }
        String address = in.get("address");
        if (!address.isEmpty() && !UNKNOWN.equals(address) && (address.length() < 5 || address.length() > 100)) {
            errors.put("address", "5-100 characters, or choose I do not know.");
        }
        String branches = in.get("branches");
        if (!branches.isEmpty() && !UNKNOWN.equals(branches) && !branches.matches("[1-9]\\d?|50")) {
            errors.put("branches", "A number from 1 to 50.");
        }
        String tin = in.get("tin");
        if (!tin.isEmpty() && !UNKNOWN.equals(tin) && !tin.matches("\\d{10}(-\\d{3})?")) {
            errors.put("tin", "10 digits, or 13 digits with a dash (1234567890-001).");
        }
        oneOf(in, errors, "type", "HOUSEHOLD", "COMPANY", "INDIVIDUAL", "UNREGISTERED");
        oneOf(in, errors, "channels", "STORE", "ONLINE", "BOTH");
        oneOf(in, errors, "registration", "REGISTERED", "IN_PROGRESS", "NONE");
        oneOf(in, errors, "method", "QUARTERLY", "MONTHLY", "OTHER");
        oneOf(in, errors, "einvoice", "YES", "NO");
        return errors;
    }

    private static void oneOf(Map<String, String> in, Map<String, String> errors, String key, String... allowed) {
        String v = in.get(key);
        if (v.isEmpty() || UNKNOWN.equals(v)) {
            return;
        }
        for (String a : allowed) {
            if (a.equals(v)) {
                return;
            }
        }
        errors.put(key, "Choose one of the listed values.");
    }

    public void saveProfile(Map<String, String> in) throws SQLException {
        for (String f : FIELDS) {
            settingDAO.set("taxp_" + f, in.get(f));
        }
        String sector = in.get("sector");
        if (sector != null && sector.matches("GOODS|SERVICE|PRODUCTION|OTHER")) {
            settingDAO.set("tax_sector", sector);
        }
    }

    public int completion(Map<String, String> p) {
        double done = 0;
        for (String f : FIELDS) {
            String v = p.get(f);
            if (v != null && !v.isEmpty()) {
                done += UNKNOWN.equals(v) ? 0.5 : 1;
            }
        }
        return (int) Math.round(done * 100 / FIELDS.length);
    }

    public List<Map<String, Object>> check(Map<String, String> p) throws SQLException {
        LocalDate today = LocalDate.now();
        String sector = p.get("sector") == null ? settingDAO.get("tax_sector", "GOODS") : p.get("sector");
        TaxParams params = taxRuleDAO.loadParams(sector, Date.valueOf(today));
        BigDecimal revenue = summaryDAO.yearRevenue(today.getYear());
        List<TaxRule> rules = taxRuleDAO.inForce(sector, Date.valueOf(today));
        boolean verified = !rules.isEmpty();
        for (TaxRule r : rules) {
            verified &= r.isVerified();
        }
        String basis = rules.isEmpty() ? "No rule in force" : rules.size() + " rules in force (" + (verified ? "all verified" : "not all verified") + ")";
        int pc = completion(p);
        String type = p.get("type");
        boolean typeKnown = !type.isEmpty() && !UNKNOWN.equals(type) && !"UNREGISTERED".equals(type);
        boolean small = "HOUSEHOLD".equals(type) || "INDIVIDUAL".equals(type);
        boolean over = params != null && revenue.compareTo(params.getThreshold()) > 0;
        boolean near = params != null && revenue.multiply(BigDecimal.valueOf(100)).compareTo(params.getThreshold().multiply(BigDecimal.valueOf(70))) >= 0;
        boolean solid = typeKnown && pc >= 70 && params != null;
        String revText = "Revenue this year " + fmt(revenue) + " VND" + (params == null ? "" : " against the threshold of " + fmt(params.getThreshold()) + " VND");
        List<Map<String, Object>> out = new ArrayList<>();
        out.add(row("VAT", "Value added tax", status(typeKnown, small, over, near, solid, "COMPANY".equals(type)), reason(typeKnown, small, over, near, solid, "COMPANY".equals(type), pc),
                conds(p, revText, over, "type", "sector", "registration", "method"), missing(p, "type", "registration", "method"), basis, verified));
        out.add(row("PIT", "Personal income tax from business", "COMPANY".equals(type) && solid ? "NOT_APPLICABLE" : status(typeKnown, small, over, near, solid, false),
                "COMPANY".equals(type) ? "A company pays corporate income tax instead." : reason(typeKnown, small, over, near, solid, false, pc),
                conds(p, revText, over, "type", "registration"), missing(p, "type", "registration"), basis, verified));
        out.add(row("EINVOICE", "Electronic invoice", "COMPANY".equals(type) || over ? "APPLICABLE" : "NEEDS_VERIFICATION",
                "COMPANY".equals(type) || over ? "Issuing e-invoices is expected for a company or above the threshold." : "May apply under the current guidance for small businesses; check before deciding.",
                conds(p, revText, over, "type", "einvoice"), missing(p, "type", "einvoice"), basis, verified));
        out.add(row("BOOKKEEPING", "Revenue book and documents", "APPLICABLE", "Every business should record revenue and keep documents.",
                new ArrayList<Object[]>(), new ArrayList<String>(), basis, verified));
        return out;
    }

    private static String status(boolean typeKnown, boolean small, boolean over, boolean near, boolean solid, boolean company) {
        if (!typeKnown) {
            return "NEEDS_VERIFICATION";
        }
        if (company) {
            return "APPLICABLE";
        }
        if (over && small) {
            return solid ? "APPLICABLE" : "NEEDS_VERIFICATION";
        }
        if (near || !solid) {
            return "NEEDS_VERIFICATION";
        }
        return "NOT_APPLICABLE";
    }

    private static String reason(boolean typeKnown, boolean small, boolean over, boolean near, boolean solid, boolean company, int pc) {
        if (!typeKnown) {
            return "The business type is unknown, so no conclusion is drawn.";
        }
        if (company) {
            return "A company declares this tax by period.";
        }
        if (over && small) {
            return solid ? "Revenue is over the threshold and the profile is complete enough." : "Revenue is over the threshold, but the profile is only " + pc + "% complete.";
        }
        if (near) {
            return "Revenue is close to the threshold; keep watching it.";
        }
        return solid ? "Revenue is below the threshold, based on the data we have." : "The profile is only " + pc + "% complete, not enough to conclude.";
    }

    private static List<Object[]> conds(Map<String, String> p, String revText, boolean over, String... keys) {
        List<Object[]> list = new ArrayList<>();
        for (String k : keys) {
            if ("sector".equals(k)) {
                list.add(new Object[]{"Business sector is set (" + p.get("sector") + ")", Boolean.TRUE});
                continue;
            }
            String v = p.get(k);
            boolean known = v != null && !v.isEmpty() && !UNKNOWN.equals(v);
            list.add(new Object[]{LABELS.get(k) + " is known", known ? Boolean.TRUE : null});
        }
        list.add(new Object[]{revText, over});
        return list;
    }

    private static List<String> missing(Map<String, String> p, String... keys) {
        List<String> list = new ArrayList<>();
        for (String k : keys) {
            String v = p.get(k);
            if (v == null || v.isEmpty() || UNKNOWN.equals(v)) {
                list.add(LABELS.get(k));
            }
        }
        return list;
    }

    private static Map<String, Object> row(String kind, String label, String status, String reason, List<Object[]> conds, List<String> missing, String basis, boolean verified) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        m.put("label", label);
        m.put("status", status);
        m.put("reason", reason);
        m.put("conds", conds);
        m.put("missing", missing);
        m.put("basis", basis);
        m.put("verified", verified);
        return m;
    }

    public String stamp(String sector, LocalDate on) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (TaxRule r : taxRuleDAO.inForce(sector, Date.valueOf(on))) {
            sb.append(r.getRuleId()).append(':').append(r.getKind()).append('=').append(r.getValue().stripTrailingZeros().toPlainString())
                    .append('@').append(r.getUpdatedAt().toString(), 0, 10).append(r.isVerified() ? "+" : "-").append(' ');
        }
        return sb.toString().trim();
    }

    public boolean rulesVerified(String sector, LocalDate on) throws SQLException {
        List<TaxRule> rules = taxRuleDAO.inForce(sector, Date.valueOf(on));
        for (TaxRule r : rules) {
            if (!r.isVerified()) {
                return false;
            }
        }
        return !rules.isEmpty();
    }

    public Map<String, Object> calculate(String sector, Date from, Date to, BigDecimal periodRevenue, BigDecimal adjustment) throws SQLException {
        LocalDate on = to.toLocalDate();
        TaxParams params = taxRuleDAO.loadParams(sector, Date.valueOf(on));
        if (params == null) {
            throw new IllegalArgumentException("Tax rules incomplete for this sector and date. Ask an administrator.");
        }
        BigDecimal taxable = periodRevenue.subtract(adjustment);
        if (taxable.signum() < 0) {
            throw new IllegalArgumentException("The adjustment cannot be larger than the revenue.");
        }
        LocalDate yearStart = LocalDate.of(on.getYear(), 1, 1);
        BigDecimal prior = from.toLocalDate().isAfter(yearStart)
                ? summaryDAO.revenueBetween(Date.valueOf(yearStart), Date.valueOf(from.toLocalDate().minusDays(1))) : BigDecimal.ZERO;
        TaxEstimate before = TaxEstimator.estimate(prior, params);
        TaxEstimate after = TaxEstimator.estimate(prior.add(taxable), params);
        BigDecimal vat = after.getVat().subtract(before.getVat()).max(BigDecimal.ZERO);
        boolean ceiling = TaxEstimate.OVER_CEILING.equals(after.getStatus());
        BigDecimal pit = ceiling ? BigDecimal.ZERO : (after.getPit() == null ? BigDecimal.ZERO : after.getPit()).subtract(before.getPit() == null ? BigDecimal.ZERO : before.getPit()).max(BigDecimal.ZERO);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("prior", prior);
        m.put("cumulative", prior.add(taxable));
        m.put("taxable", taxable);
        m.put("threshold", params.getThreshold());
        m.put("vatRate", params.getVatRatePct());
        m.put("pitRate", params.getPitRatePct());
        m.put("vat", vat);
        m.put("pit", pit);
        m.put("ceiling", ceiling);
        m.put("status", after.getStatus());
        m.put("stamp", stamp(sector, on));
        m.put("verified", rulesVerified(sector, on));
        return m;
    }

    public BigDecimal revenue(Date from, Date to) throws SQLException {
        return summaryDAO.revenueBetween(from, to);
    }

    private static String fmt(BigDecimal v) {
        return String.format("%,d", v.longValue());
    }
}
