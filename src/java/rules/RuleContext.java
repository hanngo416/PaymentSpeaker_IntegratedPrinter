package rules;

import model.Order;
import model.Transaction;

public final class RuleContext {

    private final Transaction transaction;
    private final Order order;
    private final int similarRecent;
    private final int recentCount;

    public RuleContext(Transaction transaction, Order order, int similarRecent, int recentCount) {
        this.transaction = transaction;
        this.order = order;
        this.similarRecent = similarRecent;
        this.recentCount = recentCount;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public Order getOrder() {
        return order;
    }

    public int getSimilarRecent() {
        return similarRecent;
    }

    public int getRecentCount() {
        return recentCount;
    }
}
