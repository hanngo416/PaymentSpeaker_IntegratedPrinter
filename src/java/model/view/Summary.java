package model.view;

import java.math.BigDecimal;

/**
 * Totals of one period (a shift, a day, a month or a year): what the paid orders should have brought in
 * and what the bank account really received.
 */
public class Summary {

    private String label = "";
    private int orderCount;
    private BigDecimal total = BigDecimal.ZERO;        // paid orders, full price
    private BigDecimal cash = BigDecimal.ZERO;         // part of them paid in cash
    private int bankCount;
    private BigDecimal bankReceived = BigDecimal.ZERO; // every transfer that reached the account
    private int pendingOrders;                         // orders still waiting for the customer

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public int getOrderCount() { return orderCount; }
    public void setOrderCount(int orderCount) { this.orderCount = orderCount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public BigDecimal getCash() { return cash; }
    public void setCash(BigDecimal cash) { this.cash = cash; }

    // What the paid orders should have brought into the account
    public BigDecimal getTransfer() { return total.subtract(cash); }

    public int getBankCount() { return bankCount; }
    public void setBankCount(int bankCount) { this.bankCount = bankCount; }

    public BigDecimal getBankReceived() { return bankReceived; }
    public void setBankReceived(BigDecimal bankReceived) { this.bankReceived = bankReceived; }

    // Not zero = money in the account that no paid order explains (or the other way round)
    public BigDecimal getBankDiff() { return bankReceived.subtract(getTransfer()); }

    public int getPendingOrders() { return pendingOrders; }
    public void setPendingOrders(int pendingOrders) { this.pendingOrders = pendingOrders; }
}
