import dao.OrderDAO;
import dao.StockDAO;
import dao.TransactionDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.Transaction;
import model.enums.OrderStatus;
import rules.Verdict;
import util.DBUtil;

public class StockTest {

    private static int failures = 0;

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

    private static int stock(int productId) throws Exception {
        return Integer.parseInt(one("SELECT stock_quantity FROM Products WHERE product_id = ?", productId));
    }

    private static int count(String q, Object... args) throws Exception {
        return Integer.parseInt(one(q, args));
    }

    private static Order order(int userId, String code, OrderStatus status) {
        Order o = new Order();
        o.setOrderCode(code);
        o.setCreatedBy(userId);
        o.setAmount(new BigDecimal("20000"));
        o.setCashAmount(status == OrderStatus.COMPLETED ? new BigDecimal("20000") : BigDecimal.ZERO);
        o.setDiscountAmount(BigDecimal.ZERO);
        o.setVatRate(BigDecimal.ZERO);
        o.setVatAmount(BigDecimal.ZERO);
        o.setVatIncluded(true);
        o.setStatus(status);
        o.setQrContent(null);
        return o;
    }

    private static List<OrderItem> items(int productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName("Stock test item");
        item.setUnitPrice(new BigDecimal("10000"));
        item.setQuantity(quantity);
        item.setSubtotal(new BigDecimal("10000").multiply(BigDecimal.valueOf(quantity)));
        List<OrderItem> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    private static Transaction transfer(String bankId, Integer orderId) {
        Transaction t = new Transaction();
        t.setBankTransId(bankId);
        t.setOrderId(orderId);
        t.setAmount(new BigDecimal("20000"));
        t.setContent("stock test");
        t.setBankAccount("0000");
        t.setGateway("TEST");
        t.setTransTime(new Timestamp(System.currentTimeMillis()));
        return t;
    }

    public static void main(String[] args) throws Exception {
        int productId = Integer.parseInt(one("SELECT TOP 1 product_id FROM Products WHERE active = 1 ORDER BY product_id"));
        int userId = Integer.parseInt(one("SELECT TOP 1 user_id FROM Users WHERE status = N'ACTIVE' ORDER BY user_id"));
        int startStock = stock(productId);
        int lastMovement = count("SELECT ISNULL(MAX(movement_id), 0) FROM StockMovements");
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String cashCode = "TINKY9" + suffix.substring(Math.max(0, suffix.length() - 4));
        String customCode = "TINKY8" + suffix.substring(Math.max(0, suffix.length() - 4));
        String qrCode = "TINKY7" + suffix.substring(Math.max(0, suffix.length() - 4));
        String bank1 = "STOCKTEST-" + suffix + "-A";
        String bank2 = "STOCKTEST-" + suffix + "-B";
        OrderDAO orderDAO = new OrderDAO();
        StockDAO stockDAO = new StockDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        try {
            Order cash = order(userId, cashCode, OrderStatus.COMPLETED);
            orderDAO.insert(cash, 10, items(productId, 2), null);
            check("cash order deducts the stock", stock(productId), startStock - 2);
            check("cash order writes one SALE movement", count("SELECT COUNT(*) FROM StockMovements WHERE order_id = ? AND reason = N'SALE'", cash.getOrderId()), 1);
            check("SALE movement quantity_change", one("SELECT quantity_change FROM StockMovements WHERE order_id = ?", cash.getOrderId()), "-2");
            check("SALE movement quantity_after", one("SELECT quantity_after FROM StockMovements WHERE order_id = ?", cash.getOrderId()), String.valueOf(startStock - 2));
            check("SALE movement keeps the cashier", one("SELECT created_by FROM StockMovements WHERE order_id = ?", cash.getOrderId()), String.valueOf(userId));

            Order custom = order(userId, customCode, OrderStatus.COMPLETED);
            orderDAO.insert(custom, 10, null, null);
            check("custom sale does not touch the stock", stock(productId), startStock - 2);
            check("custom sale has no movement", count("SELECT COUNT(*) FROM StockMovements WHERE order_id = ?", custom.getOrderId()), 0);

            Order qr = order(userId, qrCode, OrderStatus.PENDING);
            orderDAO.insert(qr, 10, items(productId, 3), null);
            check("pending order does not deduct", stock(productId), startStock - 2);

            transactionDAO.record(transfer(bank1, qr.getOrderId()), Verdict.legit());
            check("paid transfer deducts the stock", stock(productId), startStock - 5);
            check("paid transfer writes one SALE movement", count("SELECT COUNT(*) FROM StockMovements WHERE order_id = ? AND reason = N'SALE'", qr.getOrderId()), 1);

            transactionDAO.record(transfer(bank2, qr.getOrderId()), Verdict.legit());
            check("second transfer for the same order does not deduct again", stock(productId), startStock - 5);
            check("second transfer leaves one SALE movement", count("SELECT COUNT(*) FROM StockMovements WHERE order_id = ? AND reason = N'SALE'", qr.getOrderId()), 1);

            check("restock finds the product", stockDAO.restock(productId, 10, userId, "stock test"), true);
            check("restock adds the quantity", stock(productId), startStock + 5);
            check("restock movement", one("SELECT TOP 1 quantity_change FROM StockMovements WHERE product_id = ? AND reason = N'RESTOCK' ORDER BY movement_id DESC", productId), "10");
            check("adjust finds the product", stockDAO.adjust(productId, startStock + 100, userId, "count"), true);
            check("adjust sets the counted quantity", stock(productId), startStock + 100);
            check("adjust movement stores the difference", one("SELECT TOP 1 quantity_change FROM StockMovements WHERE product_id = ? AND reason = N'ADJUST' ORDER BY movement_id DESC", productId), "95");
            check("unknown product restock", stockDAO.restock(2147483000, 1, userId, null), false);
            check("last movement equals the stock", one("SELECT TOP 1 quantity_after FROM StockMovements WHERE product_id = ? ORDER BY movement_id DESC", productId), String.valueOf(stock(productId)));
            check("search returns the movements", stockDAO.search(productId, null, null, null, 1, 10).getTotal() >= 4, true);
            check("negative stock is allowed", stockDAO.adjust(productId, -3, userId, "oversold") && stock(productId) == -3, true);
        } finally {
            run("DELETE FROM FraudAlerts WHERE trans_id IN (SELECT trans_id FROM Transactions WHERE bank_trans_id IN (?, ?))", bank1, bank2);
            run("DELETE FROM Transactions WHERE bank_trans_id IN (?, ?)", bank1, bank2);
            run("DELETE FROM StockMovements WHERE movement_id > ?", lastMovement);
            run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code IN (?, ?, ?))", cashCode, customCode, qrCode);
            run("DELETE FROM Orders WHERE order_code IN (?, ?, ?)", cashCode, customCode, qrCode);
            run("UPDATE Products SET stock_quantity = ? WHERE product_id = ?", startStock, productId);
        }
        check("stock restored", stock(productId), startStock);
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
