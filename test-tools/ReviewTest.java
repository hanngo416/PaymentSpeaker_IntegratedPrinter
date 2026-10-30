import dao.OrderDAO;
import dao.TransactionDAO;
import dao.TransactionReviewDAO;
import filter.AccessPolicy;
import filter.RolePermissions;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Order;
import model.OrderItem;
import model.Page;
import model.Transaction;
import model.TxFilter;
import model.enums.OrderStatus;
import model.enums.Permission;
import model.enums.Role;
import model.enums.TxLabel;
import model.view.ReviewRow;
import model.view.TxRow;
import rules.Verdict;
import util.DBUtil;

public class ReviewTest {

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

    private static Order order(int userId, String code, OrderStatus status) {
        Order o = new Order();
        o.setOrderCode(code);
        o.setCreatedBy(userId);
        o.setAmount(new BigDecimal("20000"));
        o.setCashAmount(BigDecimal.ZERO);
        o.setDiscountAmount(BigDecimal.ZERO);
        o.setVatRate(BigDecimal.ZERO);
        o.setVatAmount(BigDecimal.ZERO);
        o.setVatIncluded(true);
        o.setStatus(status);
        return o;
    }

    private static List<OrderItem> items(int productId) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName("Review test item");
        item.setUnitPrice(new BigDecimal("10000"));
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("20000"));
        List<OrderItem> list = new ArrayList<>();
        list.add(item);
        return list;
    }

    private static Transaction transfer(String bankId, String amount) {
        Transaction t = new Transaction();
        t.setBankTransId(bankId);
        t.setAmount(new BigDecimal(amount));
        t.setContent("review test");
        t.setBankAccount("0000");
        t.setGateway("TEST");
        t.setTransTime(new Timestamp(System.currentTimeMillis()));
        return t;
    }

    private static String needsReview(TransactionDAO dao, String bankId) throws Exception {
        TxFilter filter = new TxFilter();
        for (int page = 1; page <= 30; page++) {
            filter.setPage(page);
            Page<TxRow> result = dao.search(filter);
            for (TxRow r : result.getItems()) {
                if (bankId.equals(r.getBankTransId())) {
                    return String.valueOf(r.isNeedsReview());
                }
            }
            if (page >= result.getTotalPages()) {
                break;
            }
        }
        return "not found";
    }

    private static int transId(String bankId) throws Exception {
        return Integer.parseInt(one("SELECT trans_id FROM Transactions WHERE bank_trans_id = ?", bankId));
    }

    private static int orderId(String code) throws Exception {
        return Integer.parseInt(one("SELECT order_id FROM Orders WHERE order_code = ?", code));
    }

    public static void main(String[] args) throws Exception {
        int productId = Integer.parseInt(one("SELECT TOP 1 product_id FROM Products WHERE active = 1 ORDER BY product_id"));
        int userId = Integer.parseInt(one("SELECT TOP 1 user_id FROM Users WHERE status = N'ACTIVE' ORDER BY user_id"));
        int startStock = Integer.parseInt(one("SELECT stock_quantity FROM Products WHERE product_id = ?", productId));
        int lastMovement = Integer.parseInt(one("SELECT ISNULL(MAX(movement_id), 0) FROM StockMovements"));
        String s = String.valueOf(System.currentTimeMillis() % 10000);
        String pendingCode = "TINKY6" + s;
        String doneCode = "TINKY5" + s;
        String[] banks = new String[6];
        for (int i = 0; i < banks.length; i++) {
            banks[i] = "REVIEWTEST-" + s + "-" + i;
        }
        OrderDAO orderDAO = new OrderDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        TransactionReviewDAO reviewDAO = new TransactionReviewDAO();
        Verdict noOrder = Verdict.flag(TxLabel.NO_ORDER, "NO_ORDER", "No order in the note", Verdict.MEDIUM);
        try {
            check("manager may resolve", RolePermissions.has(Role.STORE_MANAGER, Permission.TRANSACTION_RESOLVE), true);
            check("admin may resolve", RolePermissions.has(Role.ADMIN, Permission.TRANSACTION_RESOLVE), true);
            check("cashier may not resolve", RolePermissions.has(Role.CASHIER, Permission.TRANSACTION_RESOLVE), false);
            check("auditor may not resolve", RolePermissions.has(Role.AUDITOR, Permission.TRANSACTION_RESOLVE), false);
            check("route needs the resolve permission", AccessPolicy.requiredFor("/transactions/review", "POST"), Permission.TRANSACTION_RESOLVE);

            Order pending = order(userId, pendingCode, OrderStatus.PENDING);
            orderDAO.insert(pending, 10, items(productId), null);
            Order done = order(userId, doneCode, OrderStatus.COMPLETED);
            orderDAO.insert(done, 10, null, null);
            int pendingId = orderId(pendingCode);
            int doneId = orderId(doneCode);
            int stockAfterOrders = Integer.parseInt(one("SELECT stock_quantity FROM Products WHERE product_id = ?", productId));

            transactionDAO.record(transfer(banks[0], "10000"), noOrder);
            transactionDAO.record(transfer(banks[1], "25000"), noOrder);
            transactionDAO.record(transfer(banks[2], "20000"), noOrder);
            transactionDAO.record(transfer(banks[3], "20000"), noOrder);
            int tLow = transId(banks[0]);
            int tExcess = transId(banks[1]);
            int tClose = transId(banks[2]);
            int tSecond = transId(banks[3]);

            Page<ReviewRow> queue = reviewDAO.search("NO_ORDER", null, null, 1, 50);
            boolean listed = false;
            for (ReviewRow r : queue.getItems()) {
                listed |= banks[1].equals(r.getBankTransId());
            }
            check("flagged transfer is in the queue", listed, true);
            check("rule filter lists NO_ORDER", reviewDAO.findRuleCodes().contains("NO_ORDER"), true);
            boolean pendingListed = false;
            for (java.util.Map<String, Object> o : reviewDAO.findPendingOrders()) {
                pendingListed |= pendingCode.equals(o.get("orderCode"));
            }
            check("pending order can be chosen", pendingListed, true);

            check("transfer smaller than the order is refused", reviewDAO.link(tLow, pendingId, userId, "too small"), TransactionReviewDAO.AMOUNT_TOO_LOW);
            check("order stays pending after a refused link", one("SELECT status FROM Orders WHERE order_id = ?", pendingId), "PENDING");
            check("completed order cannot be linked", reviewDAO.link(tExcess, doneId, userId, "wrong order"), TransactionReviewDAO.ORDER_NOT_PENDING);
            check("unknown transfer", reviewDAO.link(2147483000, pendingId, userId, "missing"), TransactionReviewDAO.NOT_FOUND);

            check("link with excess succeeds", reviewDAO.link(tExcess, pendingId, userId, "customer paid 5000 more"), TransactionReviewDAO.OK);
            check("order is completed", one("SELECT status FROM Orders WHERE order_id = ?", pendingId), "COMPLETED");
            check("transfer becomes COMPLETED", one("SELECT status FROM Transactions WHERE trans_id = ?", tExcess), "COMPLETED");
            check("transfer resolution", one("SELECT resolution FROM Transactions WHERE trans_id = ?", tExcess), "LINKED");
            check("linked order id", one("SELECT linked_order_id FROM Transactions WHERE trans_id = ?", tExcess), String.valueOf(pendingId));
            check("no speaker popup after linking", one("SELECT announced FROM Transactions WHERE trans_id = ?", tExcess), "1");
            check("excess is written in the note", one("SELECT resolution_note FROM Transactions WHERE trans_id = ?", tExcess).contains("excess 5000"), true);
            check("alerts of the transfer are resolved", one("SELECT COUNT(*) FROM FraudAlerts WHERE trans_id = ? AND status IN ('NEW', 'IN_PROGRESS')", tExcess), "0");
            check("stock is deducted once", one("SELECT stock_quantity FROM Products WHERE product_id = ?", productId), String.valueOf(stockAfterOrders - 2));
            check("one SALE movement for the order", one("SELECT COUNT(*) FROM StockMovements WHERE order_id = ? AND reason = N'SALE'", pendingId), "1");
            check("same transfer cannot be linked twice", reviewDAO.link(tExcess, pendingId, userId, "again"), TransactionReviewDAO.ALREADY_RESOLVED);
            check("another transfer cannot take the completed order", reviewDAO.link(tSecond, pendingId, userId, "second payment"), TransactionReviewDAO.ORDER_NOT_PENDING);
            check("stock unchanged by refused links", one("SELECT stock_quantity FROM Products WHERE product_id = ?", productId), String.valueOf(stockAfterOrders - 2));

            check("close succeeds", reviewDAO.close(tClose, "REFUND_NEEDED", userId, "customer must be refunded"), TransactionReviewDAO.OK);
            check("close resolution", one("SELECT resolution FROM Transactions WHERE trans_id = ?", tClose), "CLOSED");
            check("close category", one("SELECT close_category FROM Transactions WHERE trans_id = ?", tClose), "REFUND_NEEDED");
            check("close keeps the transfer CANCELLED", one("SELECT status FROM Transactions WHERE trans_id = ?", tClose), "CANCELLED");
            check("close leaves other orders alone", one("SELECT status FROM Orders WHERE order_id = ?", doneId), "COMPLETED");
            check("closed twice", reviewDAO.close(tClose, "OTHER", userId, "again please"), TransactionReviewDAO.ALREADY_RESOLVED);
            check("closed transfer leaves the queue", one("SELECT COUNT(*) FROM Transactions WHERE trans_id = ? AND resolution IS NULL", tClose), "0");

            transactionDAO.record(transfer(banks[4], "9000000"), Verdict.legitWithAlert("HIGH_AMOUNT", "High amount", Verdict.HIGH));
            check("completed transfer with an open fraud alert needs review", needsReview(transactionDAO, banks[4]), "true");
            check("linked transfer does not need review", needsReview(transactionDAO, banks[1]), "false");
            run("UPDATE FraudAlerts SET status = N'RESOLVED' WHERE trans_id = ?", transId(banks[4]));
            check("the mark disappears when the alert is resolved", needsReview(transactionDAO, banks[4]), "false");
        } finally {
            String list = "('" + String.join("','", banks) + "')";
            run("DELETE FROM FraudAlerts WHERE trans_id IN (SELECT trans_id FROM Transactions WHERE bank_trans_id IN " + list + ")");
            run("DELETE FROM Transactions WHERE bank_trans_id IN " + list);
            run("DELETE FROM StockMovements WHERE movement_id > ?", lastMovement);
            run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code IN (?, ?))", pendingCode, doneCode);
            run("DELETE FROM Orders WHERE order_code IN (?, ?)", pendingCode, doneCode);
            run("UPDATE Products SET stock_quantity = ? WHERE product_id = ?", startStock, productId);
        }
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
