package rules;

import model.enums.TxLabel;

public final class Verdict {

    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String HIGH = "HIGH";

    private final TxLabel label;
    private final String ruleCode;
    private final String reason;
    private final String severity;

    private Verdict(TxLabel label, String ruleCode, String reason, String severity) {
        this.label = label;
        this.ruleCode = ruleCode;
        this.reason = reason;
        this.severity = severity;
    }

    public static Verdict legit() {
        return new Verdict(TxLabel.LEGIT, "OK", "Valid transaction", LOW);
    }

    public static Verdict flag(TxLabel label, String ruleCode, String reason, String severity) {
        return new Verdict(label, ruleCode, reason, severity);
    }

    public boolean isLegit() {
        return label == TxLabel.LEGIT;
    }

    public TxLabel getLabel() {
        return label;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getReason() {
        return reason;
    }

    public String getSeverity() {
        return severity;
    }
}
