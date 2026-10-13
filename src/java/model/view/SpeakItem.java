package model.view;

/**
 * One transaction waiting to be read out: only the two fields the speaker needs (id to confirm, amount to read).
 */
public class SpeakItem {

    private final int transId;
    private final long amount;

    public SpeakItem(int transId, long amount) {
        this.transId = transId;
        this.amount = amount;
    }

    public int getTransId() { return transId; }
    public long getAmount() { return amount; }
}
