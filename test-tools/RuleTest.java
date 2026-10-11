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
        String got = v.isLegit() ? "LEGIT" : v.getRuleCode();
        boolean ok = got.equals(expected);
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    public static void main(String[] args) {
        RulesEngine engine = RulesEngine.standard();

        // Amount against the order
        check("exact amount", engine.evaluate(new RuleContext(tx("50000.00"), order("50000", OrderStatus.PENDING), 0, 0)), "LEGIT");
        check("short by 1 VND", engine.evaluate(new RuleContext(tx("49999"), order("50000", OrderStatus.PENDING), 0, 0)), "AMOUNT_MISMATCH");
        check("short", engine.evaluate(new RuleContext(tx("40000"), order("50000", OrderStatus.PENDING), 0, 0)), "AMOUNT_MISMATCH");
        check("over-paid is not auto-matched", engine.evaluate(new RuleContext(tx("100000"), order("50000", OrderStatus.PENDING), 0, 0)), "AMOUNT_MISMATCH");

        // Order missing or closed
        check("no order", engine.evaluate(new RuleContext(tx("40000"), null, 0, 0)), "NO_ORDER");
        check("order already paid", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PAID), 0, 0)), "ORDER_NOT_PENDING");
        check("order expired", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.EXPIRED), 0, 0)), "ORDER_NOT_PENDING");

        // Rule precedence
        check("duplicate wins first", engine.evaluate(new RuleContext(tx("50000"), null, 1, 0)), "DUP_TRANS");
        check("duplicate beats amount mismatch", engine.evaluate(new RuleContext(tx("40000"), order("50000", OrderStatus.PENDING), 1, 0)), "DUP_TRANS");
        check("duplicate beats closed order", engine.evaluate(new RuleContext(tx("50000"), order("50000", OrderStatus.PAID), 1, 0)), "DUP_TRANS");

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
