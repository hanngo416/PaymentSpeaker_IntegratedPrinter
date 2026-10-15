import java.math.BigDecimal;
import model.Order;
import model.Transaction;
import model.enums.OrderStatus;
import rules.RuleContext;
import rules.RulesEngine;
import rules.Verdict;

public class RuleTest {

    private static int failures = 0;

    private static Transaction tx(String amount) {
        Transaction t = new Transaction();
        t.setAmount(new BigDecimal(amount));
        return t;
    }

    private static Order order(String amount, OrderStatus status) {
        Order o = new Order();
        o.setOrderCode("TINKY12345");
        o.setAmount(new BigDecimal(amount));
        o.setStatus(status);
        return o;
    }

    private static void check(String name, Verdict v, String expected) {
        String got = v.isLegit() && !v.needsAlert() ? "LEGIT" : v.getRuleCode();
        boolean ok = got.equals(expected);
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static void checkExtra(String name, Verdict v, String expected) {
        String got = v.getExtras().isEmpty() ? "none" : v.getExtras().get(0).getRuleCode();
        boolean ok = got.equals(expected);
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    public static void main(String[] args) {
        RulesEngine engine = RulesEngine.standard();

        // Amount against the order: QR code has fixed amount, transfer completes the order
        check("exact amount", engine.evaluate(new RuleContext(tx("50000.00"), order("50000", OrderStatus.PENDING), 0, 0)), "LEGIT");
        check("less than the QR amount is not a payment of the order", engine.evaluate(new RuleContext(tx("40000"), order("50000", OrderStatus.PENDING), 0, 0)), "AMOUNT_MISMATCH");
        check("more than the QR amount is not a payment of the order", engine.evaluate(new RuleContext(tx("60000"), order("50000", OrderStatus.PENDING), 0, 0)), "AMOUNT_MISMATCH");

        // Order missing or closed
        check("no order", engine.evaluate(new RuleContext(tx("40000"), null, 0, 0)), "NO_ORDER");
        check("order already paid", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.COMPLETED), 0, 0)), "ORDER_NOT_PENDING");
        check("order expired", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.EXPIRED), 0, 0)), "ORDER_NOT_PENDING");

        // Expired order: judged by the bank time, not by the time the server saw the transfer
        Order expired = order("50000", OrderStatus.EXPIRED);
        expired.setExpiresAt(java.sql.Timestamp.valueOf("2026-10-14 10:10:00"));
        Transaction inTime = tx("50000");
        inTime.setTransTime(java.sql.Timestamp.valueOf("2026-10-14 10:08:00"));
        Transaction tooLate = tx("50000");
        tooLate.setTransTime(java.sql.Timestamp.valueOf("2026-10-14 10:10:01"));
        check("paid in time, order swept to EXPIRED", engine.evaluate(new RuleContext(inTime, expired, 0, 0)), "LEGIT");
        check("paid after the deadline", engine.evaluate(new RuleContext(tooLate, expired, 0, 0)), "ORDER_NOT_PENDING");
        check("cancelled order never accepted", engine.evaluate(new RuleContext(inTime, order("50000", OrderStatus.CANCELLED), 0, 0)), "ORDER_NOT_PENDING");

        // Cash and transfer: the customer paid 40k in cash, the QR asks for the remaining 60k
        Order splitOrder = order("100000", OrderStatus.PENDING);
        splitOrder.setCashAmount(new BigDecimal("40000"));
        check("transfer of the QR amount completes the order", engine.evaluate(new RuleContext(tx("60000"), splitOrder, 0, 0)), "LEGIT");
        check("transfer of the full price is not the QR amount", engine.evaluate(new RuleContext(tx("100000"), splitOrder, 0, 0)), "AMOUNT_MISMATCH");

        // The watcher adds its alert on top of the main result
        Verdict bigNoOrder = engine.evaluate(new RuleContext(tx("6000000"), null, 0, 0));
        check("big transfer without an order", bigNoOrder, "NO_ORDER");
        checkExtra("big transfer without an order also raises HIGH_AMOUNT", bigNoOrder, "HIGH_AMOUNT");
        checkExtra("normal transfer has no extra alert", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PENDING), 0, 0)), "none");

        // Rule precedence
        check("duplicate wins first", engine.evaluate(new RuleContext(tx("50000"), null, 1, 0)), "DUP_TRANS");
        check("duplicate beats closed order", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.COMPLETED), 1, 0)), "DUP_TRANS");
        check("second equal transfer of an order is a duplicate", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PENDING), 1, 0)), "DUP_TRANS");

        // High amount boundary (threshold is 5,000,000 and it is inclusive)
        check("4,999,999 is allowed", engine.evaluate(new RuleContext(tx("4999999"), order("4999999", OrderStatus.PENDING), 0, 0)), "LEGIT");
        check("5,000,000 hits the threshold", engine.evaluate(new RuleContext(tx("5000000"), order("5000000", OrderStatus.PENDING), 0, 0)), "HIGH_AMOUNT");
        check("5,000,001 is flagged", engine.evaluate(new RuleContext(tx("5000001"), order("5000001", OrderStatus.PENDING), 0, 0)), "HIGH_AMOUNT");
        check("high amount", engine.evaluate(new RuleContext(tx("6000000"), order("6000000", OrderStatus.PENDING), 0, 0)), "HIGH_AMOUNT");

        // Burst boundary (5 per minute)
        check("3rd in a minute", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PENDING), 0, 2)), "LEGIT");
        check("4th in a minute", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PENDING), 0, 3)), "LEGIT");
        check("5th in a minute", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PENDING), 0, 4)), "BURST_RATE");
        check("6th in a minute", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PENDING), 0, 5)), "BURST_RATE");

        System.out.println(failures == 0 ? "ALL PASSED" : failures + " FAILED");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
