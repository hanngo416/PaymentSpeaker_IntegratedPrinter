package rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import model.Order;
import model.Transaction;
import model.enums.OrderStatus;
import model.enums.TxLabel;

public class OrderMatchRule implements Rule {

    @Override
    public Verdict evaluate(RuleContext ctx) {
        Order order = ctx.getOrder();
        Transaction tx = ctx.getTransaction();
        if (order == null) {
            return Verdict.flag(TxLabel.NO_ORDER, "NO_ORDER",
                    "No order matches the code in the transfer content.", Verdict.MEDIUM);
        }
        if (!canAccept(order, tx)) {
            return Verdict.flagWithRefund(TxLabel.ORDER_CLOSED, "ORDER_NOT_PENDING",
                    "Order " + order.getOrderCode() + " is " + order.getStatus()
                    + " and no longer waiting for payment.", Verdict.MEDIUM,
                    tx.getAmount(), "ORDER_CLOSED");
        }
        // Since VietQR has a fixed amount, transfers match the order.
        // No partial underpayment or overpayment refund logic.
        return null;
    }

    // A transfer is kept for the order while it is PENDING. An order that the sweep already marked EXPIRED still takes
    // a transfer made before the deadline, but only a full one: a part payment on an expired order was already refunded.
    public static boolean canAccept(Order order, Transaction tx) {
        if (order.getStatus() == OrderStatus.PENDING) {
            return true;
        }
        if (order.getStatus() != OrderStatus.EXPIRED || order.getExpiresAt() == null) {
            return false;
        }
        Timestamp paidAt = tx.getTransTime() != null ? tx.getTransTime() : new Timestamp(System.currentTimeMillis());
        return !paidAt.after(order.getExpiresAt());
    }

    private static String money(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}
