package rules;

import java.math.BigDecimal;
import model.Order;
import model.enums.TxLabel;

public class DuplicateRule implements Rule {

    @Override
    public Verdict evaluate(RuleContext ctx) {
        if (ctx.getSimilarRecent() > 0 && !isTopUp(ctx)) {
            String reason = "A transaction with the same amount and content arrived within the last "
                    + RuleConfig.DUPLICATE_WINDOW_SECONDS + " seconds.";
            // With an order the money can be sent back; without one nobody knows who to send it to
            if (ctx.getOrder() == null) {
                return Verdict.flag(TxLabel.DUPLICATE, "DUP_TRANS", reason, Verdict.MEDIUM);
            }
            return Verdict.flagWithRefund(TxLabel.DUPLICATE, "DUP_TRANS", reason, Verdict.MEDIUM,
                    ctx.getTransaction().getAmount(), "DUPLICATE");
        }
        return null;
    }

    // Two equal transfers in a row are normal when the order still needs both (for example 50,000 + 50,000 for a 100,000 order)
    private static boolean isTopUp(RuleContext ctx) {
        Order order = ctx.getOrder();
        if (order == null || !OrderMatchRule.canAccept(order, ctx.getTransaction())) {
            return false;
        }
        BigDecimal amount = ctx.getTransaction().getAmount();
        return amount.compareTo(order.getAmountDue()) <= 0;
    }
}
