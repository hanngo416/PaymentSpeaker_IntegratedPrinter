package model.view;

import java.math.BigDecimal;

public class ChannelProfit {

    private String channel;
    private BigDecimal revenue = BigDecimal.ZERO;
    private BigDecimal profit = BigDecimal.ZERO;
    private int orders;

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public int getOrders() {
        return orders;
    }

    public void setOrders(int orders) {
        this.orders = orders;
    }
}
