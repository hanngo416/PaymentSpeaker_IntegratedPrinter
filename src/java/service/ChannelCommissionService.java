package service;

import dao.AffiliateDAO;
import dao.ChannelCommissionDAO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import model.Affiliate;
import model.ChannelCommission;
import util.DBUtil;

public class ChannelCommissionService {

    public static final int MAX_ROWS = 5000;
    public static final int MAX_BYTES = 2 * 1024 * 1024;
    public static final long MAX_AMOUNT = 50000000L;
    public static final String PERCENT = "PERCENT";
    public static final String FIXED = "FIXED";

    private static final String[] REQUIRED = {"order_id", "order_date", "base_amount", "commission_amount", "affiliate"};
    private static final Map<String, String> SHOPEE = new HashMap<>();
    private static final Map<String, String> TIKTOK = new HashMap<>();

    static {
        for (Map<String, String> m : new Map[]{SHOPEE, TIKTOK}) {
            alias(m, "order_id", "order_id", "order_sn", "order_number", "platform_order_id", "ma_don_hang", "order_ref");
            alias(m, "commission_ref", "commission_id", "commission_ref", "commission_transaction_id", "transaction_id");
            alias(m, "order_date", "order_date", "date", "order_time", "created_at", "ngay_dat_hang");
            alias(m, "base_amount", "base_amount", "order_amount", "eligible_amount", "goods_amount", "order_value");
            alias(m, "commission_amount", "commission_amount", "commission", "affiliate_commission", "reported_commission");
            alias(m, "commission_rate", "commission_rate", "rate");
            alias(m, "affiliate", "affiliate", "affiliate_code", "referral_code", "affiliate_name", "partner_code");
        }
        alias(SHOPEE, "base_amount", "purchase_value", "actual_purchase_value");
        alias(SHOPEE, "commission_amount", "total_commission", "net_commission");
        alias(SHOPEE, "affiliate", "affiliate_id", "affiliate_username");
        alias(TIKTOK, "base_amount", "gmv", "payment_amount");
        alias(TIKTOK, "commission_amount", "est_commission", "estimated_commission", "actual_commission");
        alias(TIKTOK, "affiliate", "creator_id", "creator_name", "creator_username");
    }

    public static class Parsed implements java.io.Serializable {

        public List<ChannelCommission> rows = new ArrayList<>();
        public int duplicates;
        public List<String> errors = new ArrayList<>();
    }

    public static class Result {

        public int imported;
        public int duplicates;
    }

    private final ChannelCommissionDAO dao = new ChannelCommissionDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();

    public static boolean validChannel(String channel) {
        return "SHOPEE".equals(channel) || "TIKTOK".equals(channel);
    }

    public static BigDecimal calculate(String type, BigDecimal base, BigDecimal rate) {
        if (FIXED.equals(type)) {
            return rate.setScale(0, RoundingMode.HALF_UP);
        }
        return base.multiply(rate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    public static String rateError(String type, BigDecimal rate) {
        if (PERCENT.equals(type)) {
            if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                return "A percentage rate must be between 0 and 100.";
            }
            return null;
        }
        if (FIXED.equals(type)) {
            if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(MAX_AMOUNT)) > 0) {
                return "A fixed commission must be between 0 and 50,000,000 VND.";
            }
            return null;
        }
        return "Commission type must be PERCENT or FIXED.";
    }

    public static List<String> fillManual(ChannelCommission c, String orderId, String dateRaw, String baseRaw,
            String type, String rateRaw, Affiliate a) {
        List<String> errors = new ArrayList<>();
        if (a == null || !Affiliate.ACTIVE.equals(a.getStatus())) {
            errors.add("Choose an active affiliate.");
        } else {
            c.setAffiliateId(a.getAffiliateId());
            c.setAffiliateName(a.getName());
        }
        String id = orderId == null ? "" : orderId.trim();
        if (!id.matches("[A-Za-z0-9_.\\-]{1,60}")) {
            errors.add("Platform order ID must be 1-60 letters, digits, dots, dashes or underscores.");
        }
        c.setPlatformOrderId(id);
        c.setExternalRef(id);
        Timestamp at = SalesImportService.parseDate(dateRaw == null ? "" : dateRaw);
        if (at == null) {
            errors.add("Order date must be a date like 2026-10-01.");
        } else if (at.after(new Timestamp(System.currentTimeMillis()))) {
            errors.add("Order date cannot be in the future.");
        } else {
            c.setOrderDate(new java.sql.Date(at.getTime()));
        }
        Long base = SalesImportService.parseAmount(baseRaw == null ? "" : baseRaw);
        if (base == null || base < 1) {
            errors.add("Commission base must be a whole number from 1 to 50,000,000.");
        } else {
            c.setBaseAmount(BigDecimal.valueOf(base));
        }
        String t = type == null ? "" : type.trim();
        c.setCommissionType(t);
        BigDecimal rate = null;
        String rr = rateRaw == null ? "" : rateRaw.trim();
        if (rr.isEmpty() && PERCENT.equals(t) && a != null) {
            rate = a.getRate();
        } else if (rr.matches("\\d{1,8}(\\.\\d{1,2})?")) {
            rate = new BigDecimal(rr);
        } else {
            errors.add("Rate must be a number such as 8 or 8.5.");
        }
        if (rate != null) {
            String re = rateError(t, rate);
            if (re != null) {
                errors.add(re);
            } else {
                c.setCommissionRate(rate);
                if (c.getBaseAmount() != null) {
                    BigDecimal calc = calculate(t, c.getBaseAmount(), rate);
                    if (calc.signum() <= 0) {
                        errors.add("The commission would be 0. Check the rate.");
                    } else if (calc.compareTo(c.getBaseAmount()) > 0) {
                        errors.add("The commission cannot be larger than the base amount.");
                    } else {
                        c.setCalculatedAmount(calc);
                    }
                }
            }
        }
        c.setDataSource(ChannelCommission.MANUAL);
        c.setAttribution("Typed by the store");
        return errors;
    }

    public Parsed preview(InputStream in, String channel) throws IOException, SQLException {
        Map<String, Affiliate> byKey = new HashMap<>();
        for (Affiliate a : affiliateDAO.findAll()) {
            if (a.getReferralCode() != null) {
                byKey.put(a.getReferralCode().toLowerCase(Locale.ROOT), a);
            }
            byKey.put(a.getName().toLowerCase(Locale.ROOT), a);
        }
        Parsed parsed = parse(in, channel, byKey);
        if (!parsed.errors.isEmpty()) {
            return parsed;
        }
        Set<String> existing = dao.existingRefs(channel);
        List<ChannelCommission> fresh = new ArrayList<>();
        for (ChannelCommission c : parsed.rows) {
            if (existing.contains(c.getExternalRef().toLowerCase(Locale.ROOT))) {
                parsed.duplicates++;
            } else {
                fresh.add(c);
            }
        }
        parsed.rows = fresh;
        return parsed;
    }

    public Result commit(Parsed parsed, String channel, int actorId) throws SQLException {
        Result result = new Result();
        result.duplicates = parsed.duplicates;
        try (Connection con = DBUtil.getConnection()) {
            try {
                con.setAutoCommit(false);
                for (ChannelCommission c : parsed.rows) {
                    if (dao.existsByRef(con, channel, c.getExternalRef())) {
                        result.duplicates++;
                        continue;
                    }
                    c.setChannel(channel);
                    c.setCreatedBy(actorId);
                    dao.insert(con, c);
                    result.imported++;
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
        return result;
    }

    public static Parsed parse(InputStream in, String channel, Map<String, Affiliate> affiliates) throws IOException {
        Parsed out = new Parsed();
        Map<String, String> aliases = "TIKTOK".equals(channel) ? TIKTOK : SHOPEE;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
            if (buf.size() > MAX_BYTES) {
                out.errors.add("The file is larger than 2 MB.");
                return out;
            }
        }
        String text = new String(buf.toByteArray(), StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == (char) 0xFEFF) {
            text = text.substring(1);
        }
        SalesImportService.Parsed shell = new SalesImportService.Parsed();
        List<SalesImportService.Rec> recs = new ArrayList<>();
        if (!SalesImportService.readRecords(text, recs, shell)) {
            out.errors.addAll(shell.errors);
            return out;
        }
        if (recs.isEmpty()) {
            out.errors.add("The file is empty.");
            return out;
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> header = recs.get(0).fields;
        for (int i = 0; i < header.size(); i++) {
            String canon = aliases.get(SalesImportService.normalize(header.get(i)));
            if (canon != null && !col.containsKey(canon)) {
                col.put(canon, i);
            }
        }
        for (String r : REQUIRED) {
            if (!col.containsKey(r)) {
                out.errors.add("Missing column: " + r);
            }
        }
        if (!out.errors.isEmpty()) {
            return out;
        }
        if (recs.size() == 1) {
            out.errors.add("The file has no data rows.");
            return out;
        }
        if (recs.size() - 1 > MAX_ROWS) {
            out.errors.add("The file has more than " + MAX_ROWS + " data rows.");
            return out;
        }
        Set<String> seen = new HashSet<>();
        for (int i = 1; i < recs.size(); i++) {
            SalesImportService.Rec rec = recs.get(i);
            List<String> problems = new ArrayList<>();
            ChannelCommission c = buildRow(rec, col, affiliates, problems);
            for (String p : problems) {
                out.errors.add("Line " + rec.line + ": " + p);
            }
            if (problems.isEmpty()) {
                if (seen.add(c.getExternalRef().toLowerCase(Locale.ROOT))) {
                    out.rows.add(c);
                } else {
                    out.duplicates++;
                }
            }
        }
        if (!out.errors.isEmpty()) {
            out.rows.clear();
            out.duplicates = 0;
        }
        return out;
    }

    private static ChannelCommission buildRow(SalesImportService.Rec rec, Map<String, Integer> col,
            Map<String, Affiliate> affiliates, List<String> problems) {
        ChannelCommission c = new ChannelCommission();
        String orderId = cell(rec, col, "order_id");
        if (orderId.isEmpty()) {
            problems.add("order_id is missing.");
        } else if (orderId.length() > 60) {
            problems.add("order_id must be 1-60 characters.");
        }
        String ref = cell(rec, col, "commission_ref");
        if (ref.isEmpty()) {
            ref = orderId;
        } else if (ref.length() > 60) {
            problems.add("commission_id must be at most 60 characters.");
        }
        c.setPlatformOrderId(orderId);
        c.setExternalRef(ref);

        String key = cell(rec, col, "affiliate");
        Affiliate a = key.isEmpty() ? null : affiliates.get(key.toLowerCase(Locale.ROOT));
        if (key.isEmpty()) {
            problems.add("affiliate identifier is missing.");
        } else if (a == null) {
            problems.add("unknown affiliate '" + shorten(key) + "'.");
        } else if (!Affiliate.ACTIVE.equals(a.getStatus())) {
            problems.add("affiliate '" + shorten(key) + "' is not active.");
        } else {
            c.setAffiliateId(a.getAffiliateId());
            c.setAffiliateName(a.getName());
            c.setAttribution(shorten(key));
        }

        Timestamp at = SalesImportService.parseDate(cell(rec, col, "order_date"));
        if (at == null) {
            problems.add("order_date must be a date like 2026-10-01 or 01/10/2026.");
        } else if (at.after(new Timestamp(System.currentTimeMillis()))) {
            problems.add("order_date cannot be in the future.");
        } else {
            c.setOrderDate(new java.sql.Date(at.getTime()));
        }

        Long base = SalesImportService.parseAmount(cell(rec, col, "base_amount"));
        if (base == null || base < 1) {
            problems.add("base_amount must be a whole number from 1 to 50,000,000.");
        }
        Long reported = SalesImportService.parseAmount(cell(rec, col, "commission_amount"));
        if (reported == null || reported < 1) {
            problems.add("commission_amount must be a whole number from 1 to 50,000,000.");
        } else if (base != null && base >= 1 && reported > base) {
            problems.add("commission_amount cannot be larger than base_amount.");
        }
        BigDecimal rate = null;
        String rateRaw = cell(rec, col, "commission_rate");
        if (!rateRaw.isEmpty()) {
            if (rateRaw.endsWith("%")) {
                rateRaw = rateRaw.substring(0, rateRaw.length() - 1).trim();
            }
            if (rateRaw.matches("\\d{1,3}(\\.\\d{1,2})?")) {
                rate = new BigDecimal(rateRaw);
                if (rateError(PERCENT, rate) != null) {
                    problems.add("commission_rate must be between 0 and 100.");
                    rate = null;
                }
            } else {
                problems.add("commission_rate must be a number between 0 and 100, or empty.");
            }
        } else if (a != null) {
            rate = a.getRate();
        }
        if (problems.isEmpty()) {
            c.setBaseAmount(BigDecimal.valueOf(base));
            c.setReportedAmount(BigDecimal.valueOf(reported));
            c.setCommissionType(PERCENT);
            c.setCommissionRate(rate);
            c.setCalculatedAmount(calculate(PERCENT, c.getBaseAmount(), rate));
            c.setDataSource(ChannelCommission.CSV_IMPORT);
        }
        return c;
    }

    private static String shorten(String s) {
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    private static String cell(SalesImportService.Rec rec, Map<String, Integer> col, String name) {
        Integer i = col.get(name);
        if (i == null || i >= rec.fields.size()) {
            return "";
        }
        return rec.fields.get(i).trim();
    }

    private static void alias(Map<String, String> map, String canon, String... names) {
        for (String n : names) {
            map.put(n, canon);
        }
    }
}
