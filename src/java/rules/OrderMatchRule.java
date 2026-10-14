package rules;

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
            return Verdict.flag(TxLabel.ORDER_CLOSED, "ORDER_NOT_PENDING",
                    "Order " + order.getOrderCode() + " is " + order.getStatus()
                    + " and no longer waiting for payment.", Verdict.MEDIUM);
        }
        // The VietQR code has a fixed amount (order total minus the cash), so a different amount is not a payment of this order
        if (tx.getAmount().compareTo(order.getTransferAmount()) != 0) {
            return Verdict.flag(TxLabel.NO_ORDER, "AMOUNT_MISMATCH",
                    "Order " + order.getOrderCode() + " expects " + order.getTransferAmount().toPlainString()
                    + " VND by transfer but " + tx.getAmount().toPlainString() + " VND arrived.", Verdict.MEDIUM);
        }
        return null;
    }

    // A transfer is kept for the order while it is PENDING. An order that the sweep already marked EXPIRED still takes
    // a transfer made before the deadline.
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
}
