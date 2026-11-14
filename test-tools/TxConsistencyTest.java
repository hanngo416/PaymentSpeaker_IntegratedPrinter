import dao.OrderDAO;
import dao.TransactionDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import model.Order;
import model.OrderItem;
import model.Transaction;
import model.enums.OrderStatus;
import model.enums.TxLabel;
import rules.Verdict;
import service.TransactionIngestService;
import util.DBUtil;

public class TxConsistencyTest {

    private static int failures = 0;
    private static final int ROUNDS = 5;

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

    private static int stock(int productId) throws Exception {
        return count("SELECT stock_quantity FROM Products WHERE product_id = ?", productId);
    }

    private static String status(int orderId) throws Exception {
        return one("SELECT status FROM Orders WHERE order_id = ?", orderId);
    }

    private static int sales(int orderId) throws Exception {
        return count("SELECT COUNT(*) FROM StockMovements WHERE order_id = ? AND reason = N'SALE'", orderId);
    }

    private static Order pending(int userId, String code) {
        Order o = new Order();
        o.setOrderCode(code);
        o.setCreatedBy(userId);
        o.setAmount(new BigDecimal("20000"));
        o.setCashAmount(BigDecimal.ZERO);
        o.setDiscountAmount(BigDecimal.ZERO);
        o.setVatRate(BigDecimal.ZERO);
        o.setVatAmount(BigDecimal.ZERO);
        o.setVatIncluded(true);
        o.setStatus(OrderStatus.PENDING);
        o.setQrContent(null);
        return o;
    }

    private static List<OrderItem> items(int productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName("Consistency test item");
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
        t.setContent("consistency test");
        t.setBankAccount("0000");
        t.setGateway("TEST");
        t.setTransTime(new Timestamp(System.currentTimeMillis()));
        return t;
    }

    private static String code(int series, String suffix) {
        return "TINKY" + series + suffix.substring(Math.max(0, suffix.length() - 4));
    }

    public static void main(String[] args) throws Exception {
        int productId = Integer.parseInt(one("SELECT TOP 1 product_id FROM Products WHERE active = 1 ORDER BY product_id"));
        int userId = Integer.parseInt(one("SELECT TOP 1 user_id FROM Users WHERE status = N'ACTIVE' ORDER BY user_id"));
        int startStock = stock(productId);
        int lastMovement = count("SELECT ISNULL(MAX(movement_id), 0) FROM StockMovements");
        String suffix = String.valueOf(System.currentTimeMillis() % 100000);
        String tag = "TXCONS-" + suffix + "-";
        List<String> orderCodes = new ArrayList<>();
        OrderDAO orderDAO = new OrderDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        try {
            String codeA = code(1, suffix);
            String codeB = code(2, suffix);
            orderCodes.add(codeA);
            orderCodes.add(codeB);
            Order a = pending(userId, codeA);
            orderDAO.insert(a, 10, items(productId, 2), null);
            Order b = pending(userId, codeB);
            orderDAO.insert(b, 10, items(productId, 3), null);

            transactionDAO.record(transfer(tag + "A", a.getOrderId()), Verdict.legit());
            check("rollback: order A completed", status(a.getOrderId()), "COMPLETED");
            check("rollback: order A deducted the stock", stock(productId), startStock - 2);

            boolean raised = false;
            try {
                transactionDAO.record(transfer(tag + "A", b.getOrderId()), Verdict.legit());
            } catch (SQLException e) {
                raised = true;
            }
            check("rollback: the failing insert raises SQLException", raised, true);
            check("rollback: order B is still PENDING", status(b.getOrderId()), "PENDING");
            check("rollback: stock is not deducted for order B", stock(productId), startStock - 2);
            check("rollback: no SALE movement for order B", sales(b.getOrderId()), 0);
            check("rollback: one transaction row for the bank id", count("SELECT COUNT(*) FROM Transactions WHERE bank_trans_id = ?", tag + "A"), 1);

            String codeC = code(3, suffix);
            orderCodes.add(codeC);
            Order c = pending(userId, codeC);
            orderDAO.insert(c, 10, items(productId, 1), null);
            int beforeC = stock(productId);
            Map<String, String> payload = new HashMap<>();
            payload.put("id", tag + "C");
            payload.put("transferType", "in");
            payload.put("transferAmount", "20000");
            payload.put("content", codeC);
            payload.put("accountNumber", "0000");
            payload.put("gateway", "TEST");
            TransactionIngestService ingest = new TransactionIngestService();
            check("re-read: first notice is created", ingest.ingest(payload), TransactionIngestService.Outcome.CREATED);
            check("re-read: order C completed", status(c.getOrderId()), "COMPLETED");
            check("re-read: second identical notice is a duplicate", ingest.ingest(payload), TransactionIngestService.Outcome.DUPLICATE);
            check("re-read: third identical notice is a duplicate", ingest.ingest(payload), TransactionIngestService.Outcome.DUPLICATE);
            check("re-read: stock deducted once", stock(productId), beforeC - 1);
            check("re-read: one SALE movement", sales(c.getOrderId()), 1);
            check("re-read: one transaction row", count("SELECT COUNT(*) FROM Transactions WHERE bank_trans_id = ?", tag + "C"), 1);

            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                for (int round = 1; round <= ROUNDS; round++) {
                    String codeR = code(4, suffix + round);
                    orderCodes.add(codeR);
                    Order r = pending(userId, codeR);
                    orderDAO.insert(r, 10, items(productId, 1), null);
                    int beforeR = stock(productId);
                    final int orderId = r.getOrderId();
                    final CyclicBarrier barrier = new CyclicBarrier(2);
                    List<Future<Verdict>> futures = new ArrayList<>();
                    for (String side : new String[]{"X", "Y"}) {
                        final String bankId = tag + "R" + round + side;
                        futures.add(pool.submit(new Callable<Verdict>() {
                            @Override
                            public Verdict call() throws Exception {
                                barrier.await();
                                return transactionDAO.record(transfer(bankId, orderId), Verdict.legit());
                            }
                        }));
                    }
                    int legit = 0;
                    int closed = 0;
                    String error = "";
                    for (Future<Verdict> f : futures) {
                        try {
                            Verdict v = f.get();
                            if (v.isLegit()) {
                                legit++;
                            } else if (v.getLabel() == TxLabel.ORDER_CLOSED) {
                                closed++;
                            }
                        } catch (Exception e) {
                            error = String.valueOf(e.getCause() == null ? e : e.getCause());
                        }
                    }
                    check("overlap " + round + ": no error " + error, error, "");
                    check("overlap " + round + ": exactly one transfer completes the order", legit, 1);
                    check("overlap " + round + ": the other transfer is flagged ORDER_CLOSED", closed, 1);
                    check("overlap " + round + ": order completed", status(orderId), "COMPLETED");
                    check("overlap " + round + ": stock deducted once", stock(productId), beforeR - 1);
                    check("overlap " + round + ": one SALE movement", sales(orderId), 1);
                    check("overlap " + round + ": both transfers stored", count("SELECT COUNT(*) FROM Transactions WHERE order_id = ?", orderId), 2);
                }
            } finally {
                pool.shutdownNow();
            }
        } finally {
            run("DELETE FROM FraudAlerts WHERE trans_id IN (SELECT trans_id FROM Transactions WHERE bank_trans_id LIKE ?)", tag + "%");
            run("DELETE FROM Transactions WHERE bank_trans_id LIKE ?", tag + "%");
            run("DELETE FROM StockMovements WHERE movement_id > ?", lastMovement);
            for (String code : orderCodes) {
                run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM Orders WHERE order_code = ?", code);
            }
            run("UPDATE Products SET stock_quantity = ? WHERE product_id = ?", startStock, productId);
        }
        check("stock restored", stock(productId), startStock);
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
