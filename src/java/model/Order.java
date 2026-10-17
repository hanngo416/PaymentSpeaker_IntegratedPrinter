/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import model.enums.OrderStatus;

/**
 *
 * @author PC
 */
public class Order {

    private int orderId;
    private String orderCode;
    private int createdBy;
    private String createdByCode;
    private BigDecimal amount;
    private BigDecimal cashAmount = BigDecimal.ZERO;
    private String discountCode;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private OrderStatus status;
    private String qrContent;
    private Timestamp expiresAt;
    private Timestamp createdAt;
    private int remainingSeconds;

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public String getCreatedByCode() { return createdByCode; }
    public void setCreatedByCode(String createdByCode) { this.createdByCode = createdByCode; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getCashAmount() { return cashAmount != null ? cashAmount : BigDecimal.ZERO; }
    public void setCashAmount(BigDecimal cashAmount) { this.cashAmount = cashAmount != null ? cashAmount : BigDecimal.ZERO; }

    public BigDecimal getTransferAmount() {
        return amount != null ? amount.subtract(getCashAmount()) : BigDecimal.ZERO;
    }

    public String getDiscountCode() { return discountCode; }
    public void setDiscountCode(String discountCode) { this.discountCode = discountCode; }

    public BigDecimal getDiscountAmount() { return discountAmount != null ? discountAmount : BigDecimal.ZERO; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO; }

    public BigDecimal getSubtotal() {
        return amount != null ? amount.add(getDiscountAmount()) : BigDecimal.ZERO;
    }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public String getQrContent() { return qrContent; }
    public void setQrContent(String qrContent) { this.qrContent = qrContent; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public int getRemainingSeconds() { return remainingSeconds; }
    public void setRemainingSeconds(int remainingSeconds) { this.remainingSeconds = remainingSeconds; }
}
