package model.view;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class RefundRow {

    private int refundId;
    private String orderCode;
    private String cashierCode;
    private BigDecimal amount;
    private String reason;
    private String status;
    private String handledBy;
    private String note;
    private Timestamp createdAt;
    private Timestamp doneAt;

    public int getRefundId() { return refundId; }
    public void setRefundId(int refundId) { this.refundId = refundId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getCashierCode() { return cashierCode; }
    public void setCashierCode(String cashierCode) { this.cashierCode = cashierCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getDoneAt() { return doneAt; }
    public void setDoneAt(Timestamp doneAt) { this.doneAt = doneAt; }
}
