package model.view;

import java.math.BigDecimal;

public class Summary {

    private String label = "";
    private int orderCount;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal cash = BigDecimal.ZERO;
    private BigDecimal discount = BigDecimal.ZERO;
    private int bankCount;
    private BigDecimal bankReceived = BigDecimal.ZERO;
    private int pendingOrders;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTransfer() { return total.subtract(cash); }

    public int getBankCount() { return bankCount; }
    public void setBankCount(int bankCount) { this.bankCount = bankCount; }

    public BigDecimal getBankReceived() { return bankReceived; }
    public void setBankReceived(BigDecimal bankReceived) { this.bankReceived = bankReceived; }

    public BigDecimal getBankDiff() { return bankReceived.subtract(getTransfer()); }

    public int getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }
}
