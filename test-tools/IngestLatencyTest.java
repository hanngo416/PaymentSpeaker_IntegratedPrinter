import dao.OrderDAO;
import dao.TransactionDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.Order;
import model.enums.OrderStatus;
import model.view.SpeakItem;
import service.TransactionIngestService;
import util.DBUtil;

public class IngestLatencyTest {

    private static int failures = 0;
    private static final int WARMUP = 5;
    private static final int MATCHED = 30;
    private static final int UNMATCHED = 30;
    private static final int POLLS = 50;

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

    private static Map<String, String> payload(String id, String content) {
        Map<String, String> p = new HashMap<>();
        p.put("id", id);
        p.put("transferType", "in");
        p.put("transferAmount", "20000");
        p.put("content", content);
        p.put("accountNumber", "0000");
        p.put("gateway", "TEST");
        return p;
    }

    private static void report(String name, List<Double> ms) {
        double[] v = new double[ms.size()];
        double sum = 0;
        for (int i = 0; i < v.length; i++) {
            v[i] = ms.get(i);
            sum += v[i];
        }
        Arrays.sort(v);
        double p50 = v[v.length / 2];
        double p95 = v[Math.min(v.length - 1, (int) Math.ceil(v.length * 0.95) - 1)];
        System.out.println(String.format("TIME %-34s n=%d  mean=%.1f ms  p50=%.1f ms  p95=%.1f ms  max=%.1f ms", name, v.length, sum / v.length, p50, p95, v[v.length - 1]));
    }

    public static void main(String[] args) throws Exception {
        int userId = Integer.parseInt(one("SELECT TOP 1 user_id FROM Users WHERE status = N'ACTIVE' ORDER BY user_id"));
        String suffix = String.valueOf(System.currentTimeMillis() % 1000000);
        String tag = "LATENCY-" + suffix + "-";
        OrderDAO orderDAO = new OrderDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        TransactionIngestService ingest = new TransactionIngestService();
        List<String> orderCodes = new ArrayList<>();
        List<Double> matchedMs = new ArrayList<>();
        List<Double> unmatchedMs = new ArrayList<>();
        List<Double> pollMs = new ArrayList<>();
        try {
            for (int i = 0; i < WARMUP + MATCHED; i++) {
                String code = "TINKY" + String.format("%05d", (int) ((System.nanoTime() / 1000 + i * 7919L) % 100000));
                Order o = pending(userId, code);
                try {
                    orderDAO.insert(o, 10, null, null);
                } catch (java.sql.SQLException e) {
                    i--;
                    continue;
                }
                orderCodes.add(code);
            }
            int index = 0;
            int created = 0;
            int completed = 0;
            for (String code : orderCodes) {
                long start = System.nanoTime();
                TransactionIngestService.Outcome out = ingest.ingest(payload(tag + "M" + index, code));
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                if (out == TransactionIngestService.Outcome.CREATED) {
                    created++;
                }
                if ("COMPLETED".equals(one("SELECT status FROM Orders WHERE order_code = ?", code))) {
                    completed++;
                }
                if (index >= WARMUP) {
                    matchedMs.add(ms);
                }
                index++;
            }
            check("every matched notice was created", created, orderCodes.size());
            check("every matched order was completed", completed, orderCodes.size());

            int createdU = 0;
            for (int i = 0; i < WARMUP + UNMATCHED; i++) {
                long start = System.nanoTime();
                TransactionIngestService.Outcome out = ingest.ingest(payload(tag + "U" + i, "latency probe " + suffix + " " + i));
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                if (out == TransactionIngestService.Outcome.CREATED) {
                    createdU++;
                }
                if (i >= WARMUP) {
                    unmatchedMs.add(ms);
                }
            }
            check("every unmatched notice was stored as flagged", createdU, WARMUP + UNMATCHED);

            int visible = 0;
            for (int i = 0; i < POLLS; i++) {
                long start = System.nanoTime();
                List<SpeakItem> items = transactionDAO.findToAnnounce(200, 30, 30);
                pollMs.add((System.nanoTime() - start) / 1_000_000.0);
                if (i == 0) {
                    for (SpeakItem item : items) {
                        if (orderCodes.contains(item.getOrderCode())) {
                            visible++;
                        }
                    }
                }
            }
            check("completed orders are returned to the speaker poll", visible, orderCodes.size());

            report("ingest, matched (pays an order)", matchedMs);
            report("ingest, unmatched (flagged)", unmatchedMs);
            report("speaker poll query (findToAnnounce)", pollMs);
        } finally {
            run("DELETE FROM FraudAlerts WHERE trans_id IN (SELECT trans_id FROM Transactions WHERE bank_trans_id LIKE ?)", tag + "%");
            run("DELETE FROM Transactions WHERE bank_trans_id LIKE ?", tag + "%");
            for (String code : orderCodes) {
                run("DELETE FROM OrderItems WHERE order_id IN (SELECT order_id FROM Orders WHERE order_code = ?)", code);
                run("DELETE FROM Orders WHERE order_code = ?", code);
            }
        }
        check("test transactions removed", one("SELECT COUNT(*) FROM Transactions WHERE bank_trans_id LIKE ?", tag + "%"), "0");
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
