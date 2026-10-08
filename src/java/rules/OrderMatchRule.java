package rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import model.Order;
import model.enums.OrderStatus;
import model.enums.TxLabel;

public class OrderMatchRule implements Rule {

    @Override
    public Verdict evaluate(RuleContext ctx) {
        Order order = ctx.getOrder();
        if (order == null) {
            return Verdict.flag(TxLabel.AMOUNT_MISMATCH, "NO_ORDER",
                    "No order matches the code in the transfer content.", Verdict.MEDIUM);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            return Verdict.flag(TxLabel.AMOUNT_MISMATCH, "ORDER_NOT_PENDING",
                    "Order " + order.getOrderCode() + " is " + order.getStatus()
                    + " and no longer waiting for payment.", Verdict.MEDIUM);
        }
        BigDecimal paid = ctx.getTransaction().getAmount();
        BigDecimal expected = order.getAmount();
        int cmp = paid.compareTo(expected);
        if (cmp != 0) {
            BigDecimal diff = paid.subtract(expected).abs().setScale(0, RoundingMode.HALF_UP);
            return Verdict.flag(TxLabel.AMOUNT_MISMATCH, "AMOUNT_MISMATCH",
                    "Order " + order.getOrderCode() + " needs "
                    + expected.setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + " VND but " + paid.setScale(0, RoundingMode.HALF_UP).toPlainString()
                    + " VND was received (" + (cmp < 0 ? "short by " : "over by ")
                    + diff.toPlainString() + " VND).", Verdict.MEDIUM);
        }
        return null;
    }
}
