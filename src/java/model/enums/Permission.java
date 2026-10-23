/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package model.enums;

/**
 *
 * @author PC
 */
public enum Permission {
    DASHBOARD_READ("DASHBOARD:READ"),
    ORDER_CREATE("ORDER:CREATE"),
    SPEAKER_USE("SPEAKER:USE"),
    SHIFT_USE("SHIFT:USE"),
    SHIFT_PRINT("SHIFT:PRINT"),
    INVOICE_READ("INVOICE:READ"),
    TRANSACTION_READ("TRANSACTION:READ"),
    ALERT_READ("ALERT:READ"),
    ALERT_WRITE("ALERT:WRITE"),
    AUDIT_READ("AUDIT:READ"),
    USER_READ("USER:READ"),
    USER_WRITE("USER:WRITE"),
    DEVICE_READ("DEVICE:READ"),
    DEVICE_WRITE("DEVICE:WRITE"),
    DISCOUNT_MANAGE("DISCOUNT:MANAGE"),
    PRODUCT_MANAGE("PRODUCT:MANAGE"),
    TAX_READ("TAX:READ"),
    TAX_MANAGE("TAX:MANAGE"),
    SALES_MANAGE("SALES:MANAGE"),
    AFFILIATE_PORTAL("AFFILIATE:PORTAL"),
    AFFILIATE_FINANCE_READ("AFFILIATE:FINANCE_READ"),
    SYSTEM_ADMIN("SYSTEM:ADMIN");

    private final String code;

    Permission(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
