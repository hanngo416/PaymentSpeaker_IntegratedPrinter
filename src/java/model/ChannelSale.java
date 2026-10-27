package model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ChannelSale {

    private int saleId;
    private String channel;
    private String externalRef;
    private Timestamp soldAt;
    private String status;
    private BigDecimal goodsAmount;
    private BigDecimal platformFee;
    private BigDecimal shippingFee;
    private Integer affiliateId;
    private BigDecimal affiliateCommission;
    private BigDecimal costAmount;
    private String note;
    private int createdBy;
    private Timestamp createdAt;
    private String affiliateName;

    private BigDecimal buyerPaidAmount = BigDecimal.ZERO;
    private BigDecimal sellerDiscount = BigDecimal.ZERO;
    private BigDecimal platformDiscount = BigDecimal.ZERO;
    private BigDecimal serviceFee = BigDecimal.ZERO;
    private BigDecimal transactionFee = BigDecimal.ZERO;
    private Timestamp settledAt;
    private BigDecimal settlementAmount;
    private String settlementStatus = "SETTLED";
    private String trafficSource;
    private String cancelReason;
    private java.util.List<ChannelSaleItem> items = new java.util.ArrayList<>();

    public ChannelSale() {
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getExternalRef() {
        return externalRef;
    }

    public void setExternalRef(String externalRef) {
        this.externalRef = externalRef;
    }

    public Timestamp getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(Timestamp soldAt) {
        this.soldAt = soldAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getGoodsAmount() {
        return goodsAmount;
    }

    public void setGoodsAmount(BigDecimal goodsAmount) {
        this.goodsAmount = goodsAmount;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(BigDecimal shippingFee) {
        this.shippingFee = shippingFee;
    }

    public Integer getAffiliateId() {
        return affiliateId;
    }

    public void setAffiliateId(Integer affiliateId) {
        this.affiliateId = affiliateId;
    }

    public BigDecimal getAffiliateCommission() {
        return affiliateCommission;
    }

    public void setAffiliateCommission(BigDecimal affiliateCommission) {
        this.affiliateCommission = affiliateCommission;
    }

    public BigDecimal getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(BigDecimal costAmount) {
        this.costAmount = costAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getAffiliateName() {
        return affiliateName;
    }

    public void setAffiliateName(String affiliateName) {
        this.affiliateName = affiliateName;
    }

    public BigDecimal getBuyerPaidAmount() {
        return buyerPaidAmount;
    }

    public void setBuyerPaidAmount(BigDecimal buyerPaidAmount) {
        this.buyerPaidAmount = buyerPaidAmount;
    }

    public BigDecimal getSellerDiscount() {
        return sellerDiscount;
    }

    public void setSellerDiscount(BigDecimal sellerDiscount) {
        this.sellerDiscount = sellerDiscount;
    }

    public BigDecimal getPlatformDiscount() {
        return platformDiscount;
    }

    public void setPlatformDiscount(BigDecimal platformDiscount) {
        this.platformDiscount = platformDiscount;
    }

    public BigDecimal getServiceFee() {
        return serviceFee;
    }

    public void setServiceFee(BigDecimal serviceFee) {
        this.serviceFee = serviceFee;
    }

    public BigDecimal getTransactionFee() {
        return transactionFee;
    }

    public void setTransactionFee(BigDecimal transactionFee) {
        this.transactionFee = transactionFee;
    }

    public Timestamp getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(Timestamp settledAt) {
        this.settledAt = settledAt;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public String getTrafficSource() {
        return trafficSource;
    }

    public void setTrafficSource(String trafficSource) {
        this.trafficSource = trafficSource;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public java.util.List<ChannelSaleItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<ChannelSaleItem> items) {
        this.items = items;
    }
}
