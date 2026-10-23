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
                    + (order.getStatus() == OrderStatus.PENDING ? " but had already expired when the transfer was made." : " and no longer waiting for payment."),
                    Verdict.MEDIUM);
        }
        if (tx.getAmount().compareTo(order.getTransferAmount()) != 0) {
            return Verdict.flag(TxLabel.NO_ORDER, "AMOUNT_MISMATCH",
                    "Order " + order.getOrderCode() + " expects " + order.getTransferAmount().toPlainString()
                    + " VND by transfer but " + tx.getAmount().toPlainString() + " VND arrived.", Verdict.MEDIUM);
        }
        return null;
    }

    public static boolean canAccept(Order order, Transaction tx) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.EXPIRED) {
            return false;
        }
        if (order.getExpiresAt() == null) {
            return order.getStatus() == OrderStatus.PENDING;
        }
        Timestamp paidAt = tx.getTransTime() != null ? tx.getTransTime() : new Timestamp(System.currentTimeMillis());
        return !paidAt.after(order.getExpiresAt());
    }
}
