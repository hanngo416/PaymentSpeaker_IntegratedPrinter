package model.view;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ReceiptRow {

    private int transId;
    private int orderId;
    private String bankTransId;
    private String orderCode;
    private String cashierCode;
    private BigDecimal amount;
    private BigDecimal orderAmount;
    private Timestamp transTime;
    private boolean printed;

    public int getTransId() { return transId; }
    public void setTransId(int transId) { this.transId = transId; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getBankTransId() { return bankTransId; }
    public void setBankTransId(String bankTransId) { this.bankTransId = bankTransId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public String getCashierCode() { return cashierCode; }
    public void setCashierCode(String cashierCode) { this.cashierCode = cashierCode; }

    // The transfer this receipt is for
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }

    private BigDecimal cashAmount = BigDecimal.ZERO;
    public BigDecimal getCashAmount() { return cashAmount != null ? cashAmount : BigDecimal.ZERO; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount != null ? cashAmount : BigDecimal.ZERO; }

    private BigDecimal vatRate = BigDecimal.ZERO;
    public BigDecimal getVatRate() { return vatRate != null ? vatRate : BigDecimal.ZERO; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate != null ? vatRate : BigDecimal.ZERO; }

    public BigDecimal getVatAmount() {
        if (orderAmount == null || getVatRate().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return orderAmount.multiply(getVatRate()).divide(getVatRate().add(BigDecimal.valueOf(100)), 0, java.math.RoundingMode.HALF_UP);
    }

    public Timestamp getTransTime() { return transTime; }
    public void setTransTime(Timestamp transTime) { this.transTime = transTime; }

    public boolean isPrinted() { return printed; }
    public void setPrinted(boolean printed) { this.printed = printed; }
}
