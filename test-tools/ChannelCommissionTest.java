import dao.AffiliateDAO;
import dao.ChannelCommissionDAO;
import dao.ChannelPayoutDAO;
import filter.AccessPolicy;
import filter.RolePermissions;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.Affiliate;
import model.ChannelCommission;
import model.ChannelPayout;
import model.enums.Permission;
import model.enums.Role;
import service.ChannelCommissionService;
import util.DBUtil;

public class ChannelCommissionTest {

    private static int failures = 0;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static InputStream in(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static void sql(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
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

    private static void cleanup() throws Exception {
        sql("DELETE FROM ChannelPayoutItems WHERE commission_id IN (SELECT commission_id FROM ChannelCommissions WHERE external_ref LIKE 'TMP-CH-%')");
        sql("UPDATE ChannelCommissions SET payout_id = NULL WHERE external_ref LIKE 'TMP-CH-%'");
        sql("DELETE FROM ChannelPayouts WHERE notes = 'TMP-CH-TEST'");
        sql("DELETE FROM ChannelCommissions WHERE external_ref LIKE 'TMP-CH-%'");
    }

    private static int idOf(String ref) throws Exception {
        return Integer.parseInt(one("SELECT commission_id FROM ChannelCommissions WHERE external_ref = ?", ref));
    }

    public static void main(String[] args) throws Exception {
        check("percent 400000 x 8%", ChannelCommissionService.calculate("PERCENT", BigDecimal.valueOf(400000), new BigDecimal("8")), "32000");
        check("percent rounds half up 333 x 5%", ChannelCommissionService.calculate("PERCENT", BigDecimal.valueOf(333), new BigDecimal("5")), "17");
        check("fixed amount", ChannelCommissionService.calculate("FIXED", BigDecimal.valueOf(400000), new BigDecimal("10000")), "10000");
        check("rate 101 refused", ChannelCommissionService.rateError("PERCENT", new BigDecimal("101")) != null, true);
        check("rate -1 refused", ChannelCommissionService.rateError("PERCENT", new BigDecimal("-1")) != null, true);
        check("rate 100 allowed", ChannelCommissionService.rateError("PERCENT", new BigDecimal("100")), "null");
        check("fixed above 50,000,000 refused", ChannelCommissionService.rateError("FIXED", BigDecimal.valueOf(50000001)) != null, true);
        check("unknown type refused", ChannelCommissionService.rateError("OTHER", BigDecimal.ONE) != null, true);

        Map<String, Affiliate> byKey = new HashMap<>();
        Affiliate linh = null;
        Affiliate minh = null;
        for (Affiliate a : new AffiliateDAO().findAll()) {
            byKey.put(a.getName().toLowerCase(Locale.ROOT), a);
            if (a.getReferralCode() != null) {
                byKey.put(a.getReferralCode().toLowerCase(Locale.ROOT), a);
            }
            if ("Linh Review".equals(a.getName())) {
                linh = a;
            } else if ("Minh Unbox".equals(a.getName())) {
                minh = a;
            }
        }
        String head = "order_id,commission_id,order_date,order_amount,commission_amount,commission_rate,affiliate\n";
        ChannelCommissionService.Parsed ok = ChannelCommissionService.parse(in("﻿" + head
                + "TMP-CH-1,,2026-10-01,400000,32000,8,LINH08\r\n"
                + "TMP-CH-2,CM-77,01/10/2026,\"1,000,000\",50000,,minh unbox\r\n"
                + "TMP-CH-1,,2026-10-01,400000,32000,8,LINH08\r\n"), "SHOPEE", byKey);
        check("csv: no errors", ok.errors, "[]");
        check("csv: 2 rows, 1 duplicate in file", ok.rows.size() + "/" + ok.duplicates, "2/1");
        check("csv: platform amount kept as reported", ok.rows.get(0).getReportedAmount(), "32000");
        check("csv: rate of the affiliate used when empty", ok.rows.get(1).getCommissionRate(), minh.getRate());
        check("csv: commission_id is the record reference", ok.rows.get(1).getExternalRef(), "CM-77");

        ChannelCommissionService.Parsed tiktok = ChannelCommissionService.parse(in(
                "order_id,order_date,payment_amount,est_commission,creator_id\n" + "TMP-CH-T1,2026-10-02,300000,24000,MINH05\n"), "TIKTOK", byKey);
        check("csv: tiktok profile columns", tiktok.errors.isEmpty() && tiktok.rows.size() == 1, true);
        ChannelCommissionService.Parsed wrongProfile = ChannelCommissionService.parse(in(
                "order_id,order_date,payment_amount,est_commission,creator_id\n" + "X,2026-10-02,300000,24000,MINH05\n"), "SHOPEE", byKey);
        check("csv: shopee profile refuses tiktok columns", wrongProfile.errors.isEmpty(), false);

        ChannelCommissionService.Parsed bad = ChannelCommissionService.parse(in(head
                + ",,2026-10-01,400000,32000,8,LINH08\n"
                + "A1,,2026-10-01,0,32000,8,LINH08\n"
                + "A2,,2026-13-45,400000,32000,8,LINH08\n"
                + "A3,,2026-10-01,400000,32000,150,LINH08\n"
                + "A4,,2026-10-01,400000,32000,8,NOBODY\n"
                + "A5,,2026-10-01,400000,32000,8,\n"
                + "A6,,2026-10-01,400000,500000,8,LINH08\n"
                + "A7,,2999-01-01,400000,32000,8,LINH08\n"
                + "A8,,2026-10-01,abc,32000,8,LINH08\n"), "SHOPEE", byKey);
        check("csv validation: 9 problems found", bad.errors.size(), 9);
        check("csv validation: nothing kept", bad.rows.size(), 0);
        check("csv: missing column", ChannelCommissionService.parse(in("order_id,order_date\nA,2026-10-01\n"), "SHOPEE", byKey).errors.get(0).startsWith("Missing column"), true);
        check("csv: empty file", ChannelCommissionService.parse(in(""), "SHOPEE", byKey).errors.get(0), "The file is empty.");

        ChannelCommission m = new ChannelCommission();
        List<String> manualErrors = ChannelCommissionService.fillManual(m, "TMP-CH-M", "2026-10-01", "500000", "PERCENT", "", linh);
        check("manual: no errors and rate taken from the affiliate", manualErrors.isEmpty() && m.getCommissionRate().compareTo(linh.getRate()) == 0, true);
        check("manual: calculated amount", m.getCalculatedAmount(), ChannelCommissionService.calculate("PERCENT", BigDecimal.valueOf(500000), linh.getRate()));
        check("manual: rate 120 refused", ChannelCommissionService.fillManual(new ChannelCommission(), "TMP-CH-M", "2026-10-01", "500000", "PERCENT", "120", linh).isEmpty(), false);
        Affiliate suspended = new Affiliate();
        suspended.setStatus(Affiliate.SUSPENDED);
        check("manual: inactive affiliate refused", ChannelCommissionService.fillManual(new ChannelCommission(), "TMP-CH-M", "2026-10-01", "500000", "PERCENT", "5", suspended).isEmpty(), false);

        check("rbac: auditor reads, cannot write", RolePermissions.has(Role.AUDITOR, Permission.AFFILIATE_FINANCE_READ) + "/" + RolePermissions.has(Role.AUDITOR, Permission.SALES_MANAGE), "true/false");
        check("rbac: manager reads and writes", RolePermissions.has(Role.STORE_MANAGER, Permission.AFFILIATE_FINANCE_READ) + "/" + RolePermissions.has(Role.STORE_MANAGER, Permission.SALES_MANAGE), "true/true");
        check("rbac: cashier and technician have neither", RolePermissions.has(Role.CASHIER, Permission.AFFILIATE_FINANCE_READ) + "/" + RolePermissions.has(Role.TECHNICIAN, Permission.AFFILIATE_FINANCE_READ), "false/false");
        check("rbac: payouts GET needs read", AccessPolicy.requiredFor("/admin/channel-payouts", "GET"), Permission.AFFILIATE_FINANCE_READ);
        check("rbac: payouts POST needs write", AccessPolicy.requiredFor("/admin/channel-payouts", "POST"), Permission.SALES_MANAGE);
        check("rbac: import POST needs write", AccessPolicy.requiredFor("/admin/channel-commissions/import", "POST"), Permission.SALES_MANAGE);

        cleanup();
        ChannelCommissionService service = new ChannelCommissionService();
        ChannelCommissionDAO dao = new ChannelCommissionDAO();
        ChannelPayoutDAO payouts = new ChannelPayoutDAO();
        int admin = Integer.parseInt(one("SELECT user_id FROM Users WHERE user_code = 'han_admin'"));
        int manager = Integer.parseInt(one("SELECT user_id FROM Users WHERE user_code = 'mai_manager'"));

        String file = head
                + "TMP-CH-A,,2026-10-01,400000,32000,8,LINH08\n"
                + "TMP-CH-B,,2026-10-02,250000,12500,5,LINH08\n"
                + "TMP-CH-C,,2026-10-03,600000,30000,5,MINH05\n";
        ChannelCommissionService.Parsed p1 = service.preview(in(file), "SHOPEE");
        check("import: preview has 3 new rows", p1.rows.size() + "/" + p1.duplicates, "3/0");
        ChannelCommissionService.Result r1 = service.commit(p1, "SHOPEE", admin);
        check("import: 3 saved", r1.imported + "/" + r1.duplicates, "3/0");
        ChannelCommissionService.Parsed p2 = service.preview(in(file), "SHOPEE");
        check("import again: every row is a duplicate", p2.rows.size() + "/" + p2.duplicates, "0/3");
        ChannelCommissionService.Result r2 = service.commit(p1, "SHOPEE", admin);
        check("import again with stale preview: nothing saved twice", r2.imported + "/" + r2.duplicates, "0/3");
        check("import: same order on TikTok is a different record", service.preview(in(file), "TIKTOK").rows.size(), 3);
        check("import: stored as PENDING with source CSV_IMPORT",
                one("SELECT status + '/' + data_source FROM ChannelCommissions WHERE external_ref = 'TMP-CH-A'"), "PENDING/CSV_IMPORT");

        int a = idOf("TMP-CH-A");
        int b = idOf("TMP-CH-B");
        int c = idOf("TMP-CH-C");
        check("approve PENDING", dao.approve(a, manager), true);
        check("approved amount = platform reported amount", one("SELECT approved_amount FROM ChannelCommissions WHERE commission_id = ?", a), "32000.00");
        check("approve again refused", dao.approve(a, manager), false);
        check("reject an APPROVED commission refused", dao.reject(a, manager, "too late"), false);
        check("reject PENDING with reason", dao.reject(c, manager, "Order returned by the buyer"), true);
        check("rejected commission cannot be approved", dao.approve(c, manager), false);
        check("cancel a PENDING commission refused", dao.cancel(b, manager, "not approved yet"), false);
        sql("UPDATE Affiliates SET status = 'SUSPENDED' WHERE affiliate_id = ?", linh.getAffiliateId());
        check("approve refused when the partner is suspended", dao.approve(b, manager), false);
        sql("UPDATE Affiliates SET status = 'ACTIVE' WHERE affiliate_id = ?", linh.getAffiliateId());
        check("approve B", dao.approve(b, manager), true);

        BigDecimal rateBefore = dao.find(a).getCommissionRate();
        sql("UPDATE Affiliates SET rate = 20 WHERE affiliate_id = ?", linh.getAffiliateId());
        check("history: changing the partner rate does not change an old commission", dao.find(a).getCommissionRate().compareTo(rateBefore) == 0, true);
        sql("UPDATE Affiliates SET rate = ? WHERE affiliate_id = ?", linh.getRate(), linh.getAffiliateId());

        check("outstanding lists approved unreserved commissions", payouts.outstanding().stream().filter(x -> x.getPlatformOrderId().startsWith("TMP-CH-")).count(), 2L);
        check("draft refuses commissions of another partner", payouts.createDraft(minh.getAffiliateId(), Arrays.asList(a), "BANK_TRANSFER", "TMP-CH-TEST", manager), 0);
        check("draft refuses a PENDING/REJECTED commission", payouts.createDraft(linh.getAffiliateId(), Arrays.asList(a, c), "BANK_TRANSFER", "TMP-CH-TEST", manager), 0);
        check("failed draft left nothing reserved", one("SELECT COUNT(*) FROM ChannelCommissions WHERE payout_id IS NOT NULL AND external_ref LIKE 'TMP-CH-%'"), "0");
        int draft = payouts.createDraft(linh.getAffiliateId(), Arrays.asList(a, b), "BANK_TRANSFER", "TMP-CH-TEST", manager);
        ChannelPayout dp = payouts.find(draft);
        check("draft total = sum of its items", dp.getTotalAmount() + "/" + dp.getItemCount() + "/" + dp.getStatus(), "44500.00/2/DRAFT");
        check("draft reference is generated", dp.getReference().startsWith("CP-"), true);
        check("a commission cannot join a second draft", payouts.createDraft(linh.getAffiliateId(), Arrays.asList(a), "CASH", "TMP-CH-TEST", manager), 0);
        check("commission in a draft cannot be cancelled", dao.cancel(a, manager, "should fail now"), false);
        check("remove one item from the draft", payouts.removeItem(draft, b), true);
        check("draft total follows the items", payouts.find(draft).getTotalAmount(), "32000.00");
        check("the last item cannot be removed", payouts.removeItem(draft, a), false);
        check("cancel the draft", payouts.cancel(draft), true);
        check("cancelled draft released its commissions", one("SELECT COUNT(*) FROM ChannelCommissions WHERE payout_id IS NOT NULL AND external_ref LIKE 'TMP-CH-%'"), "0");
        check("cancelled payout cannot be confirmed", payouts.confirm(draft, "CASH", "REF-0001", new java.sql.Date(System.currentTimeMillis()), null, admin), false);

        int d2 = payouts.createDraft(linh.getAffiliateId(), Arrays.asList(a, b), "BANK_TRANSFER", "TMP-CH-TEST", manager);
        sql("UPDATE ChannelPayoutItems SET amount = amount + 1 WHERE payout_id = ? AND commission_id = ?", d2, a);
        check("confirm refused when an item no longer matches (rolled back)", payouts.confirm(d2, "BANK_TRANSFER", "REF-0002", new java.sql.Date(System.currentTimeMillis()), null, admin), false);
        check("rollback: payout still DRAFT and commissions still APPROVED",
                one("SELECT status FROM ChannelPayouts WHERE payout_id = ?", d2) + "/" + one("SELECT COUNT(*) FROM ChannelCommissions WHERE payout_id = ? AND status = 'APPROVED'", d2), "DRAFT/2");
        sql("UPDATE ChannelPayoutItems SET amount = amount - 1 WHERE payout_id = ? AND commission_id = ?", d2, a);
        check("confirm refused when the total differs from the items", tamperTotalAndConfirm(payouts, d2, admin), false);
        check("confirm payout", payouts.confirm(d2, "BANK_TRANSFER", "REF-0003", new java.sql.Date(System.currentTimeMillis()), null, admin), true);
        check("confirmed: commissions are PAID", one("SELECT COUNT(*) FROM ChannelCommissions WHERE payout_id = ? AND status = 'PAID'", d2), "2");
        check("confirmed payout by", one("SELECT status + '/' + payment_reference FROM ChannelPayouts WHERE payout_id = ?", d2), "CONFIRMED/REF-0003");
        check("confirm twice refused", payouts.confirm(d2, "CASH", "REF-0004", new java.sql.Date(System.currentTimeMillis()), null, admin), false);
        check("cancel a confirmed payout refused", payouts.cancel(d2), false);
        check("remove an item from a confirmed payout refused", payouts.removeItem(d2, a), false);
        check("a paid commission cannot go into another payout", payouts.createDraft(linh.getAffiliateId(), Arrays.asList(a), "CASH", "TMP-CH-TEST", manager), 0);
        check("a paid commission cannot be cancelled or approved", dao.cancel(a, manager, "cannot do this") + "/" + dao.approve(a, manager), "false/false");

        cleanup();
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean tamperTotalAndConfirm(ChannelPayoutDAO payouts, int id, int admin) throws Exception {
        sql("UPDATE ChannelPayouts SET total_amount = total_amount + 5 WHERE payout_id = ?", id);
        boolean result = payouts.confirm(id, "BANK_TRANSFER", "REF-0005", new java.sql.Date(System.currentTimeMillis()), null, admin);
        sql("UPDATE ChannelPayouts SET total_amount = total_amount - 5 WHERE payout_id = ?", id);
        return result;
    }
}
