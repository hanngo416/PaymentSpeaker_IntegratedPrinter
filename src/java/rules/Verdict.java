package rules;

import java.util.Collections;
import java.util.List;
import model.enums.TxLabel;

public final class Verdict {

    public static final String LOW = "LOW";
    public static final String MEDIUM = "MEDIUM";
    public static final String HIGH = "HIGH";

    private static final List<Verdict> NONE = Collections.emptyList();

    private final TxLabel label;
    private final String ruleCode;
    private final String reason;
    private final String severity;
    private final boolean alert;
    private final List<Verdict> extras;

    private Verdict(TxLabel label, String ruleCode, String reason, String severity, boolean alert,
            List<Verdict> extras) {
        this.label = label;
        this.ruleCode = ruleCode;
        this.reason = reason;
        this.severity = severity;
        this.alert = alert;
        this.extras = extras;
    }

    public static Verdict legit() {
        return new Verdict(TxLabel.LEGIT, "OK", "Valid transaction", LOW, false, NONE);
    }

    public static Verdict flag(TxLabel label, String ruleCode, String reason, String severity) {
        return new Verdict(label, ruleCode, reason, severity, true, NONE);
    }

    // The payment is valid (order paid, popup shown) but an alert is still raised for the auditor to review
    public static Verdict legitWithAlert(String ruleCode, String reason, String severity) {
        return new Verdict(TxLabel.LEGIT, ruleCode, reason, severity, true, NONE);
    }

    // Alerts raised besides the main result (for example a high amount on top of a closed order)
    public Verdict withExtras(List<Verdict> more) {
        return new Verdict(label, ruleCode, reason, severity, alert, Collections.unmodifiableList(more));
    }

    public List<Verdict> getExtras() {
        return extras;
    }

    public boolean isLegit() {
        return label == TxLabel.LEGIT;
    }

    public boolean needsAlert() {
        return alert;
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
