import dao.SettingDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Attribution;
import model.Order;
import model.User;
import model.enums.Role;
import service.AffiliateService;
import service.OrderService;
import util.DBUtil;

public class AffiliateTest {

    private static int failures = 0;
    private static final List<String> codes = new ArrayList<>();

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
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

    private static String money(String raw) {
        return raw == null ? null : new BigDecimal(raw).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static User user(String code, Role role) throws Exception {
        User u = new User();
        u.setUserId(Integer.parseInt(one("SELECT user_id FROM Users WHERE user_code = ?", code)));
        u.setUserCode(code);
        u.setRole(role);
        return u;
    }

    private static Order order(OrderService svc, User u, String amount, boolean allCash, String discount, String affiliateCode) throws Exception {
        Order o = svc.create(u, amount, "", allCash, "", discount, null, affiliateCode);
        codes.add(o.getOrderCode());
        return o;
    }

    private static String refuses(AffiliateService svc, String code, User u, long net) throws Exception {
        try {
            svc.attribute(code, u, net);
            return "accepted";
        } catch (IllegalArgumentException e) {
            return "refused";
        }
    }

    private static void addAffiliate(String name, String code, String status) throws Exception {
        run("INSERT INTO Affiliates (name, rate, referral_code, status, active) VALUES (?, 0, ?, ?, ?)", name, code, status, "ACTIVE".equals(status) ? 1 : 0);
    }

    public static void main(String[] args) throws Exception {
        OrderService orders = new OrderService();
        AffiliateService affiliates = new AffiliateService();
        SettingDAO settings = new SettingDAO();
        User cashier = user("nam_cashier", Role.CASHIER);
        User admin = user("han_admin", Role.ADMIN);
        String oldRate = settings.get("vat_rate", "0");
        String oldIncluded = settings.get("vat_included", "1");
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        String active = "TXA" + suffix;
        String pending = "TXP" + suffix;
        String suspended = "TXS" + suffix;
        String campaign = "TX campaign " + suffix;
        try {
            settings.set("vat_rate", "8");
            settings.set("vat_included", "0");
            addAffiliate("TX active " + suffix, active, "ACTIVE");
            addAffiliate("TX pending " + suffix, pending, "PENDING");
            addAffiliate("TX suspended " + suffix, suspended, "SUSPENDED");
            run("INSERT INTO Campaigns (name, description, commission_type, commission_value, min_order, start_date, end_date, status, created_by) "
                    + "VALUES (?, N'test', N'PERCENT', 50, 80000, DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), N'ACTIVE', ?)", campaign, admin.getUserId());
            int affiliateId = count("SELECT affiliate_id FROM Affiliates WHERE referral_code = ?", active);
            int campaignId = count("SELECT campaign_id FROM Campaigns WHERE name = ?", campaign);

            check("blank code means a normal sale", affiliates.attribute("", cashier, 100000), null);
            check("null code means a normal sale", affiliates.attribute(null, cashier, 100000), null);
            check("spaces only means a normal sale", affiliates.attribute("   ", cashier, 100000), null);
            Attribution a = affiliates.attribute(active, cashier, 100000);
            check("active affiliate is credited", a.getAffiliate().getAffiliateId(), affiliateId);
            check("the campaign paying the most wins", a.getCampaign().getCampaignId(), campaignId);
            check("commission is 50% of the eligible amount", a.getCommission(), 50000L);
            check("rule snapshot names the campaign", a.getSnapshot().contains(campaign), true);
            check("lowercase code is accepted", affiliates.attribute(active.toLowerCase(), cashier, 100000).getAffiliate().getAffiliateId(), affiliateId);
            Attribution fixed = affiliates.attribute(active, cashier, 60000);
            check("below the minimum of one campaign the other one pays", fixed.getCommission(), 10000L);
            check("campaign values are not stacked", fixed.getCampaign().getCampaignId() == campaignId, false);
            check("unknown code is refused", refuses(affiliates, "NOSUCH" + suffix, cashier, 100000), "refused");
            check("malformed code is refused", refuses(affiliates, "a!", cashier, 100000), "refused");
            check("pending affiliate is refused", refuses(affiliates, pending, cashier, 100000), "refused");
            check("suspended affiliate is refused", refuses(affiliates, suspended, cashier, 100000), "refused");
            check("order below every campaign minimum is refused", refuses(affiliates, active, cashier, 40000), "refused");

            Order withCode = order(orders, cashier, "100000", false, "", active);
            check("order: amount is net plus VAT", money(one("SELECT amount FROM Orders WHERE order_id = ?", withCode.getOrderId())), "108000");
            check("order: introducer stored", count("SELECT referred_by_affiliate_id FROM Orders WHERE order_id = ?", withCode.getOrderId()), affiliateId);
            check("order: campaign stored", count("SELECT referral_campaign_id FROM Orders WHERE order_id = ?", withCode.getOrderId()), campaignId);
            check("order: seller stays the cashier", count("SELECT created_by FROM Orders WHERE order_id = ?", withCode.getOrderId()), cashier.getUserId());
            check("order: one commission row", count("SELECT COUNT(*) FROM Commissions WHERE order_id = ?", withCode.getOrderId()), 1);
            check("commission: PENDING", one("SELECT status FROM Commissions WHERE order_id = ?", withCode.getOrderId()), "PENDING");
            check("commission: base is the amount before VAT", money(one("SELECT eligible_amount FROM Commissions WHERE order_id = ?", withCode.getOrderId())), "100000");
            check("commission: order amount kept", money(one("SELECT order_amount FROM Commissions WHERE order_id = ?", withCode.getOrderId())), "108000");
            check("commission: amount", money(one("SELECT amount FROM Commissions WHERE order_id = ?", withCode.getOrderId())), "50000");
            check("commission: original amount", money(one("SELECT original_amount FROM Commissions WHERE order_id = ?", withCode.getOrderId())), "50000");
            check("commission: rule text frozen", one("SELECT rule_snapshot FROM Commissions WHERE order_id = ?", withCode.getOrderId()).contains(campaign), true);
            check("order: audit row names the affiliate code", one("SELECT detail_json FROM AuditLogs WHERE action = N'CREATE_ORDER' AND entity_id = ?", withCode.getOrderCode()).contains(active), true);

            Order withDiscount = order(orders, cashier, "100000", false, "20000", active);
            check("discount: commission base is after the discount", money(one("SELECT eligible_amount FROM Commissions WHERE order_id = ?", withDiscount.getOrderId())), "80000");
            check("discount: commission is 50% of the base", money(one("SELECT amount FROM Commissions WHERE order_id = ?", withDiscount.getOrderId())), "40000");

            Order normal = order(orders, cashier, "100000", false, "", "");
            check("normal sale: no introducer", one("SELECT referred_by_affiliate_id FROM Orders WHERE order_id = ?", normal.getOrderId()), null);
            check("normal sale: no campaign", one("SELECT referral_campaign_id FROM Orders WHERE order_id = ?", normal.getOrderId()), null);
            check("normal sale: no commission row", count("SELECT COUNT(*) FROM Commissions WHERE order_id = ?", normal.getOrderId()), 0);

            int ordersBefore = count("SELECT COUNT(*) FROM Orders");
            String result;
            try {
                orders.create(cashier, "100000", "", false, "", "", null, suspended);
                result = "created";
            } catch (IllegalArgumentException e) {
                result = "refused";
            }
            check("a refused code creates no order", result, "refused");
            check("a refused code leaves the order count unchanged", count("SELECT COUNT(*) FROM Orders"), ordersBefore);

            check("cancel the referred order", orders.cancel(cashier, withCode.getOrderCode()), true);
            check("cancel: commission REVERSED", one("SELECT status FROM Commissions WHERE order_id = ?", withCode.getOrderId()), "REVERSED");
            check("cancel: commission amount 0", money(one("SELECT amount FROM Commissions WHERE order_id = ?", withCode.getOrderId())), "0");
            check("cancel: original amount kept", money(one("SELECT original_amount FROM Commissions WHERE order_id = ?", withCode.getOrderId())), "50000");
            check("cancel: reason written", one("SELECT reason FROM Commissions WHERE order_id = ?", withCode.getOrderId()), "Order cancelled");
            check("cancel: audit row written", count("SELECT COUNT(*) FROM AuditLogs WHERE action = N'CANCEL_ORDER' AND entity_id = ?", withCode.getOrderCode()), 1);

            Order cashReferred = order(orders, cashier, "100000", true, "", active);
            check("cash order with a code: completed", one("SELECT status FROM Orders WHERE order_id = ?", cashReferred.getOrderId()), "COMPLETED");
            check("cash order with a code: commission PENDING", one("SELECT status FROM Commissions WHERE order_id = ?", cashReferred.getOrderId()), "PENDING");
            check("a completed order cannot be cancelled", orders.cancel(admin, cashReferred.getOrderCode()), false);
            check("so its commission stays PENDING", one("SELECT status FROM Commissions WHERE order_id = ?", cashReferred.getOrderId()), "PENDING");
            check("one commission per order is enforced by the database", count("SELECT COUNT(*) FROM (SELECT order_id FROM Commissions WHERE order_id = ? GROUP BY order_id HAVING COUNT(*) > 1) x", cashReferred.getOrderId()), 0);
        } finally {
            for (String code : codes) {
                run("DELETE FROM Commissions WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM Orders WHERE order_code = ?", code);
            }
            run("DELETE FROM Campaigns WHERE name = ?", campaign);
            run("DELETE FROM Affiliates WHERE referral_code IN (?, ?, ?)", active, pending, suspended);
            settings.set("vat_rate", oldRate);
            settings.set("vat_included", oldIncluded);
        }
        check("test affiliates removed", count("SELECT COUNT(*) FROM Affiliates WHERE referral_code LIKE ?", "TX_" + suffix), 0);
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
