/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.math.BigDecimal;
import java.sql.Timestamp;
import model.enums.TxLabel;

/**
 *
 * @author PC
 */
public class Transaction {

    private int transId;
    private String bankTransId;
    private Integer orderId;
    private Integer deviceId;
    private BigDecimal amount;
    private String content;
    private String bankAccount;
    private String gateway;
    private TxLabel status = TxLabel.LEGIT;
    private Timestamp transTime;

    public int getTransId() { return transId; }
    public void setTransId(int transId) { this.transId = transId; }

    public String getBankTransId() { return bankTransId; }
    public void setBankTransId(String bankTransId) { this.bankTransId = bankTransId; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getDeviceId() { return deviceId; }
    public void setDeviceId(Integer deviceId) { this.deviceId = deviceId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getBankAccount() { return bankAccount; }
    public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }

    public String getGateway() { return gateway; }
    public void setGateway(String gateway) { this.gateway = gateway; }

    public TxLabel getStatus() { return status; }
    public void setStatus(TxLabel status) { this.status = status; }

    public Timestamp getTransTime() { return transTime; }
    public void setTransTime(Timestamp transTime) { this.transTime = transTime; }
}
