package rules;

import java.math.BigDecimal;
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
    private final BigDecimal credit;
    private final BigDecimal refund;
    private final String refundReason;
    private final List<Verdict> extras;

    private Verdict(TxLabel label, String ruleCode, String reason, String severity, boolean alert,
            BigDecimal credit, BigDecimal refund, String refundReason, List<Verdict> extras) {
        this.label = label;
        this.ruleCode = ruleCode;
        this.reason = reason;
        this.severity = severity;
        this.alert = alert;
        this.credit = credit;
        this.refund = refund;
        this.refundReason = refundReason;
        this.extras = extras;
    }

    public static Verdict legit() {
        return new Verdict(TxLabel.LEGIT, "OK", "Valid transaction", LOW, false, null, BigDecimal.ZERO, null, NONE);
    }

    public static Verdict flag(TxLabel label, String ruleCode, String reason, String severity) {
        return new Verdict(label, ruleCode, reason, severity, true, null, BigDecimal.ZERO, null, NONE);
    }

    // The money is not kept for the order, so the whole amount has to go back to the customer
    public static Verdict flagWithRefund(TxLabel label, String ruleCode, String reason, String severity,
            BigDecimal refund, String refundReason) {
        return new Verdict(label, ruleCode, reason, severity, true, null, refund, refundReason, NONE);
    }

    // The payment is valid (order paid, speaker reads it) but an alert is still raised for the auditor to review
    public static Verdict legitWithAlert(String ruleCode, String reason, String severity) {
        return new Verdict(TxLabel.LEGIT, ruleCode, reason, severity, true, null, BigDecimal.ZERO, null, NONE);
    }

    // Part of the order was paid: the money is added to the order, the order stays open, no alert
    public static Verdict partial(BigDecimal credit, String reason) {
        return new Verdict(TxLabel.PARTIAL, "PARTIAL", reason, LOW, false, credit, BigDecimal.ZERO, null, NONE);
    }

    // Paid more than the order needs: the order is paid, the extra money goes back to the customer
    public static Verdict overpaid(BigDecimal credit, BigDecimal refund, String reason) {
        return new Verdict(TxLabel.LEGIT, "OVERPAID", reason, MEDIUM, true, credit, refund, "OVERPAID", NONE);
    }

    // Alerts raised besides the main result (for example a high amount on top of a part payment)
    public Verdict withExtras(List<Verdict> more) {
        return new Verdict(label, ruleCode, reason, severity, alert, credit, refund, refundReason,
                Collections.unmodifiableList(more));
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

    // Money added to the order; null means the whole transfer
    public BigDecimal getCredit() {
        return credit;
    }

    public BigDecimal getRefund() {
        return refund;
    }

    public String getRefundReason() {
        return refundReason;
    }
}
