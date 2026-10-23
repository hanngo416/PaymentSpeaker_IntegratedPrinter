package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;

public class Campaign {

    public static final String PERCENT = "PERCENT";
    public static final String FIXED = "FIXED";

    private int campaignId;
    private String name;
    private String description;
    private String commissionType;
    private BigDecimal commissionValue;
    private BigDecimal minOrder;
    private Date startDate;
    private Date endDate;
    private String status;
    private int orderCount;
    private BigDecimal revenue = BigDecimal.ZERO;
    private Timestamp createdAt;

    public Campaign() {
    }

    public long commissionFor(long eligible) {
        long amount;
        if (PERCENT.equals(commissionType)) {
            amount = BigDecimal.valueOf(eligible).multiply(commissionValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValue();
        } else {
            amount = commissionValue.setScale(0, RoundingMode.HALF_UP).longValue();
        }
        return Math.min(amount, eligible);
    }

    public String rateText() {
        if (PERCENT.equals(commissionType)) {
            return commissionValue.stripTrailingZeros().toPlainString() + "%";
        }
        return String.format("%,d", commissionValue.longValue()) + " VND per order";
    }

    public int getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(int campaignId) {
        this.campaignId = campaignId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCommissionType() {
        return commissionType;
    }

    public void setCommissionType(String commissionType) {
        this.commissionType = commissionType;
    }

    public BigDecimal getCommissionValue() {
        return commissionValue;
    }

    public void setCommissionValue(BigDecimal commissionValue) {
        this.commissionValue = commissionValue;
    }

    public BigDecimal getMinOrder() {
        return minOrder;
    }

    public void setMinOrder(BigDecimal minOrder) {
        this.minOrder = minOrder;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
