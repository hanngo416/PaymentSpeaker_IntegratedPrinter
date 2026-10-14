package model.view;

import java.sql.Timestamp;

/**
 * One payment waiting for the cashier to confirm in the popup: id to confirm, amount, order code and bank time to show.
 */
public class SpeakItem {

    private final int transId;
    private final long amount;
    private final String orderCode;
    private final Timestamp transTime;

    public SpeakItem(int transId, long amount, String orderCode, Timestamp transTime) {
        this.transId = transId;
        this.amount = amount;
        this.orderCode = orderCode;
        this.transTime = transTime;
    }

    public int getTransId() { return transId; }
    public long getAmount() { return amount; }
    public String getOrderCode() { return orderCode; }
    public Timestamp getTransTime() { return transTime; }
}
