/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filter;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import model.enums.Permission;
import model.enums.Role;

import static model.enums.Permission.*;

/**
 *
 * @author PC
 */
public final class RolePermissions {

    private static final Map<Role, Set<Permission>> MAP = new EnumMap<>(Role.class);

    static {
        MAP.put(Role.ADMIN, EnumSet.allOf(Permission.class));
        MAP.put(Role.STORE_MANAGER, EnumSet.of(
                DASHBOARD_READ, TRANSACTION_READ, TRANSACTION_RESOLVE, ALERT_READ,
                USER_READ, USER_WRITE, DEVICE_READ, SPEAKER_USE, SHIFT_USE, SHIFT_PRINT, DISCOUNT_MANAGE, PRODUCT_MANAGE, TAX_READ, SALES_MANAGE, AFFILIATE_FINANCE_READ, AFFILIATE_PORTAL));
        MAP.put(Role.CASHIER, EnumSet.of(
                DASHBOARD_READ, ORDER_CREATE, SPEAKER_USE, TRANSACTION_READ, SHIFT_USE, SHIFT_PRINT, AFFILIATE_PORTAL));
        MAP.put(Role.AUDITOR, EnumSet.of(
                DASHBOARD_READ, TRANSACTION_READ, ALERT_READ, ALERT_WRITE, AUDIT_READ, SHIFT_PRINT, INVOICE_READ, AFFILIATE_FINANCE_READ, AFFILIATE_PORTAL));
        MAP.put(Role.TECHNICIAN, EnumSet.of(
                DASHBOARD_READ, DEVICE_READ, DEVICE_WRITE, AFFILIATE_PORTAL));
        MAP.put(Role.AFFILIATE, EnumSet.of(
                DASHBOARD_READ, AFFILIATE_PORTAL));
    }

    private RolePermissions() {
    }

    public static boolean has(Role role, Permission permission) {
        Set<Permission> granted = MAP.get(role);
        return granted != null && granted.contains(permission);
    }
}
