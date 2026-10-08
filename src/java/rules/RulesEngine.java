package rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RulesEngine {

    private final List<Rule> rules;

    public RulesEngine(List<Rule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    public static RulesEngine standard() {
        return new RulesEngine(Arrays.<Rule>asList(
                new DuplicateRule(),
                new OrderMatchRule(),
                new FraudRule()));
    }

    public Verdict evaluate(RuleContext ctx) {
        for (Rule rule : rules) {
            Verdict verdict = rule.evaluate(ctx);
            if (verdict != null) {
                return verdict;
            }
        }
        return Verdict.legit();
    }
}
