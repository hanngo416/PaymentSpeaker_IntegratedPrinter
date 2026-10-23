package rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RulesEngine {

    private final List<Rule> rules;
    private final Rule watcher;

    public RulesEngine(List<Rule> rules, Rule watcher) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
        this.watcher = watcher;
    }

    public static RulesEngine standard() {
        return new RulesEngine(Arrays.<Rule>asList(
                new DuplicateRule(),
                new OrderMatchRule()),
                new FraudRule());
    }

    public Verdict evaluate(RuleContext ctx) {
        Verdict main = null;
        for (Rule rule : rules) {
            main = rule.evaluate(ctx);
            if (main != null) {
                break;
            }
        }
        Verdict watched = watcher.evaluate(ctx);
        if (main == null) {
            return watched != null ? watched : Verdict.legit();
        }
        return watched == null ? main : main.withExtras(Collections.singletonList(watched));
    }
}
