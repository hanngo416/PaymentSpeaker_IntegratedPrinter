package rules;

public interface Rule {

    Verdict evaluate(RuleContext ctx);
}
