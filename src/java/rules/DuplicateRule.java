package rules;

import model.enums.TxLabel;

public class DuplicateRule implements Rule {

    @Override
    public Verdict evaluate(RuleContext ctx) {
        if (ctx.getSimilarRecent() > 0) {
            return Verdict.flag(TxLabel.DUPLICATE, "DUP_TRANS",
                    "A transaction with the same amount and content arrived within the last "
                    + RuleConfig.DUPLICATE_WINDOW_SECONDS + " seconds.", Verdict.MEDIUM);
        }
        return null;
    }
}
