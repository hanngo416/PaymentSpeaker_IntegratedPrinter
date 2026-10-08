package rules;

import model.enums.TxLabel;

public class FraudRule implements Rule {

    @Override
    public Verdict evaluate(RuleContext ctx) {
        if (ctx.getTransaction().getAmount().compareTo(RuleConfig.HIGH_AMOUNT) >= 0) {
            return Verdict.flag(TxLabel.SUSPECTED_FRAUD, "HIGH_AMOUNT",
                    "Amount is " + RuleConfig.HIGH_AMOUNT.toPlainString()
                    + " VND or more and needs a manual check.", Verdict.HIGH);
        }
        if (ctx.getRecentCount() + 1 >= RuleConfig.BURST_LIMIT) {
            return Verdict.flag(TxLabel.SUSPECTED_FRAUD, "BURST_RATE",
                    RuleConfig.BURST_LIMIT + " or more transactions within "
                    + RuleConfig.BURST_WINDOW_SECONDS + " seconds.", Verdict.HIGH);
        }
        return null;
    }
}
