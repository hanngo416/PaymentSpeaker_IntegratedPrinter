import dao.SettingDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.User;
import model.enums.Role;
import service.OrderService;
import util.AppConfig;
import util.DBUtil;

public class OrderCreateTest {

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

    private static String col(Order o, String column) throws Exception {
        return one("SELECT " + column + " FROM Orders WHERE order_id = ?", o.getOrderId());
    }

    private static String money(String raw) {
        return new BigDecimal(raw).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static User user(String code, Role role) throws Exception {
        User u = new User();
        u.setUserId(Integer.parseInt(one("SELECT user_id FROM Users WHERE user_code = ?", code)));
        u.setUserCode(code);
        u.setRole(role);
        return u;
    }

    private static Order make(OrderService svc, User u, String amount, String cash, boolean allCash, String code, String discount, List<OrderItem> items) throws Exception {
        Order o = svc.create(u, amount, cash, allCash, code, discount, items);
        codes.add(o.getOrderCode());
        return o;
    }

    private static void rejects(String name, OrderService svc, User u, String amount, String cash, boolean allCash, String code, String discount, List<OrderItem> items) throws Exception {
        String result;
        try {
            Order o = svc.create(u, amount, cash, allCash, code, discount, items);
            codes.add(o.getOrderCode());
            result = "created " + o.getOrderCode();
        } catch (IllegalArgumentException e) {
            result = "rejected";
        }
        check(name, result, "rejected");
    }

    private static List<OrderItem> cart(Integer productId, int quantity, long fakePrice) {
        List<OrderItem> list = new ArrayList<>();
        list.add(new OrderItem(0, productId, "tampered name", BigDecimal.valueOf(fakePrice), quantity, BigDecimal.valueOf(fakePrice * quantity)));
        return list;
    }

    public static void main(String[] args) throws Exception {
        OrderService svc = new OrderService();
        SettingDAO settings = new SettingDAO();
        User cashier = user("nam_cashier", Role.CASHIER);
        User otherCashier = user("ha_cashier", Role.CASHIER);
        User admin = user("han_admin", Role.ADMIN);
        String oldRate = settings.get("vat_rate", "0");
        String oldIncluded = settings.get("vat_included", "1");
        int productId = Integer.parseInt(one("SELECT TOP 1 product_id FROM Products WHERE active = 1 ORDER BY product_id"));
        BigDecimal price = new BigDecimal(one("SELECT price FROM Products WHERE product_id = ?", productId));
        int startStock = Integer.parseInt(one("SELECT stock_quantity FROM Products WHERE product_id = ?", productId));
        int lastMovement = count("SELECT ISNULL(MAX(movement_id), 0) FROM StockMovements");
        String discountCode = "TXTEST" + (System.currentTimeMillis() % 100000);
        try {
            settings.set("vat_rate", "8");
            settings.set("vat_included", "0");
            run("INSERT INTO DiscountCodes (code, kind, value, holder, max_uses, valid_from, valid_to) "
                    + "VALUES (?, N'PERCENT', 10, NULL, 1, DATEADD(DAY, -1, CAST(GETDATE() AS DATE)), DATEADD(DAY, 30, CAST(GETDATE() AS DATE)))", discountCode);

            Order qr = make(svc, cashier, "100000", "", false, "", "", null);
            check("QR order: status", qr.getStatus(), "PENDING");
            check("QR order: VAT added on top, amount", money(col(qr, "amount")), "108000");
            check("QR order: VAT amount", money(col(qr, "vat_amount")), "8000");
            check("QR order: no cash part", money(col(qr, "cash_amount")), "0");
            check("QR order: no discount", money(col(qr, "discount_amount")), "0");
            check("QR order: QR content carries the code", col(qr, "qr_content").contains(qr.getOrderCode()), true);
            check("QR order: QR asks for the whole amount", col(qr, "qr_content").contains("108000"), true);
            check("QR order: code format", qr.getOrderCode().matches("TINKY\\d{5}"), true);
            check("QR order: expires in the future", count("SELECT CASE WHEN expires_at > GETDATE() THEN 1 ELSE 0 END FROM Orders WHERE order_id = ?", qr.getOrderId()), 1);
            check("QR order: no stock line for a custom sale", count("SELECT COUNT(*) FROM OrderItems WHERE order_id = ?", qr.getOrderId()), 0);
            check("QR order: audit row written once", count("SELECT COUNT(*) FROM AuditLogs WHERE action = N'CREATE_ORDER' AND entity_id = ?", qr.getOrderCode()), 1);
            check("QR order: seller is the cashier", count("SELECT created_by FROM Orders WHERE order_id = ?", qr.getOrderId()), cashier.getUserId());

            Order cash = make(svc, cashier, "100000", "", true, "", "", null);
            check("cash order: completed at once", cash.getStatus(), "COMPLETED");
            check("cash order: no QR", col(cash, "qr_content"), null);
            check("cash order: cash equals amount", money(col(cash, "cash_amount")), money(col(cash, "amount")));

            Order split = make(svc, cashier, "100000", "30000", false, "", "", null);
            check("split order: waits for the transfer", split.getStatus(), "PENDING");
            check("split order: cash part stored", money(col(split, "cash_amount")), "30000");
            check("split order: QR asks only for the rest", col(split, "qr_content").contains("78000"), true);

            Order typed = make(svc, cashier, "100000", "", false, "", "20000", null);
            check("typed discount: amount after discount and VAT", money(col(typed, "amount")), "86400");
            check("typed discount: stored", money(col(typed, "discount_amount")), "20000");
            check("typed discount: VAT on the net", money(col(typed, "vat_amount")), "6400");
            check("typed discount: no code stored", col(typed, "discount_code"), null);

            Order coded = make(svc, cashier, "100000", "", false, discountCode, "", null);
            check("code 10%: amount", money(col(coded, "amount")), "97200");
            check("code 10%: discount", money(col(coded, "discount_amount")), "10000");
            check("code 10%: code stored", col(coded, "discount_code"), discountCode);
            check("code 10%: use counted", count("SELECT used_count FROM DiscountCodes WHERE code = ?", discountCode), 1);
            rejects("code with max uses 1 cannot be used again", svc, cashier, "100000", "", false, discountCode, "", null);
            check("code cancelled order releases the use", svc.cancel(cashier, coded.getOrderCode()), true);
            check("code: use given back", count("SELECT used_count FROM DiscountCodes WHERE code = ?", discountCode), 0);
            Order again = make(svc, cashier, "100000", "", false, discountCode, "", null);
            check("code usable again after the release", col(again, "discount_code"), discountCode);

            rejects("code and typed discount together", svc, cashier, "100000", "", false, "SUMMER10", "5000", null);
            rejects("unknown discount code", svc, cashier, "100000", "", false, "NOSUCHCODE", "", null);
            rejects("typed discount that leaves less than the minimum", svc, cashier, "100000", "", false, "", "99500", null);
            rejects("typed discount with letters", svc, cashier, "100000", "", false, "", "12ab", null);

            rejects("amount with letters", svc, cashier, "abc", "", false, "", "", null);
            rejects("empty amount", svc, cashier, "", "", false, "", "", null);
            rejects("negative amount", svc, cashier, "-5000", "", false, "", "", null);
            rejects("amount below the minimum", svc, cashier, String.valueOf(AppConfig.MIN_AMOUNT - 1), "", false, "", "", null);
            rejects("amount above the maximum", svc, cashier, String.valueOf(AppConfig.MAX_AMOUNT + 1), "", false, "", "", null);
            rejects("total with VAT above the maximum", svc, cashier, String.valueOf(AppConfig.MAX_AMOUNT), "", false, "", "", null);
            rejects("cash part above the amount", svc, cashier, "100000", "200000", false, "", "", null);
            rejects("negative cash part", svc, cashier, "100000", "-1", false, "", "", null);

            Order edge = make(svc, cashier, String.valueOf(AppConfig.MIN_AMOUNT), "", true, "", "", null);
            check("minimum amount is accepted", money(col(edge, "amount")), money(new BigDecimal(AppConfig.MIN_AMOUNT * 108 / 100).toPlainString()));

            settings.set("vat_included", "1");
            Order included = make(svc, cashier, "100000", "", false, "", "", null);
            check("VAT included: amount stays the price", money(col(included, "amount")), "100000");
            check("VAT included: VAT part of the price", money(col(included, "vat_amount")), "7407");
            settings.set("vat_included", "0");

            Order cartOrder = make(svc, cashier, "", "", true, "", "", cart(productId, 2, 1));
            BigDecimal net = price.multiply(BigDecimal.valueOf(2));
            BigDecimal expectedAmount = net.add(net.multiply(BigDecimal.valueOf(8)).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP));
            check("cart: price comes from the catalog, not from the browser", money(col(cartOrder, "amount")), money(expectedAmount.toPlainString()));
            check("cart: item price snapshot", money(one("SELECT unit_price FROM OrderItems WHERE order_id = ?", cartOrder.getOrderId())), money(price.toPlainString()));
            check("cart: item name comes from the catalog", one("SELECT product_name FROM OrderItems WHERE order_id = ?", cartOrder.getOrderId()).equals("tampered name"), false);
            check("cart: cash order deducts the stock", count("SELECT stock_quantity FROM Products WHERE product_id = ?", productId), startStock - 2);

            rejects("cart with an unknown product", svc, cashier, "", "", true, "", "", cart(2147483000, 1, 1000));
            rejects("cart with quantity 0", svc, cashier, "", "", true, "", "", cart(productId, 0, 1000));
            rejects("cart with quantity 1000", svc, cashier, "", "", true, "", "", cart(productId, 1000, 1000));
            rejects("cart with no product id", svc, cashier, "", "", true, "", "", cart(null, 1, 1000));

            Order mine = make(svc, cashier, "100000", "", false, "", "", null);
            check("other cashier cannot cancel the order", svc.cancel(otherCashier, mine.getOrderCode()), false);
            check("order is still PENDING", col(mine, "status"), "PENDING");
            check("other cashier cannot see the order", svc.findForUser(otherCashier, mine.getOrderCode()) == null, true);
            check("admin can see the order", svc.findForUser(admin, mine.getOrderCode()) != null, true);
            check("owner cancels the order", svc.cancel(cashier, mine.getOrderCode()), true);
            check("order is CANCELLED", col(mine, "status"), "CANCELLED");
            check("cancelling again does nothing", svc.cancel(cashier, mine.getOrderCode()), false);
            check("a completed cash order cannot be cancelled", svc.cancel(admin, cash.getOrderCode()), false);
            check("completed order stays COMPLETED", col(cash, "status"), "COMPLETED");
            Order forAdmin = make(svc, cashier, "100000", "", false, "", "", null);
            check("admin can cancel any pending order", svc.cancel(admin, forAdmin.getOrderCode()), true);
        } finally {
            for (String code : codes) {
                run("DELETE FROM Commissions WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM StockMovements WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM Orders WHERE order_code = ?", code);
            }
            run("DELETE FROM StockMovements WHERE movement_id > ?", lastMovement);
            run("DELETE FROM DiscountCodes WHERE code = ?", discountCode);
            run("UPDATE Products SET stock_quantity = ? WHERE product_id = ?", startStock, productId);
            settings.set("vat_rate", oldRate);
            settings.set("vat_included", oldIncluded);
        }
        check("stock restored", count("SELECT stock_quantity FROM Products WHERE product_id = ?", productId), startStock);
        check("VAT rate restored", settings.get("vat_rate", ""), oldRate);
        check("VAT mode restored", settings.get("vat_included", ""), oldIncluded);
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
