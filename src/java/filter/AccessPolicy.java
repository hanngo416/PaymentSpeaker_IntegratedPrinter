/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filter;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import model.enums.Permission;

import static model.enums.Permission.*;

/**
 *
 * @author PC
 */
public final class AccessPolicy {

    private static final class Rule {
        final String prefix;
        final Permission read;
        final Permission write;

        Rule(String prefix, Permission read, Permission write) {
            this.prefix = prefix;
            this.read = read;
            this.write = write;
        }
    }

    private static final List<Rule> RULES = Arrays.asList(
            new Rule("/admin/users/edit", USER_WRITE, USER_WRITE),
            new Rule("/admin/users", USER_READ, USER_WRITE),
            new Rule("/admin/devices", DEVICE_READ, DEVICE_WRITE),
            new Rule("/admin/discounts", DISCOUNT_MANAGE, DISCOUNT_MANAGE),
            new Rule("/admin/products", PRODUCT_MANAGE, PRODUCT_MANAGE),
            new Rule("/admin/audit", AUDIT_READ, AUDIT_READ),
            new Rule("/admin", SYSTEM_ADMIN, SYSTEM_ADMIN),
            new Rule("/shift/print", SHIFT_PRINT, SHIFT_PRINT),
            new Rule("/shift", SHIFT_USE, SHIFT_USE),
            new Rule("/invoices", INVOICE_READ, INVOICE_READ),
            new Rule("/orders", ORDER_CREATE, ORDER_CREATE),
            new Rule("/speaker", SPEAKER_USE, SPEAKER_USE),
            new Rule("/printer", SPEAKER_USE, SPEAKER_USE),
            new Rule("/transactions", TRANSACTION_READ, TRANSACTION_READ),
            new Rule("/alerts", ALERT_READ, ALERT_WRITE),
            new Rule("/settings", DASHBOARD_READ, DASHBOARD_READ),
            new Rule("/home", DASHBOARD_READ, DASHBOARD_READ),
            new Rule("/dashboard", DASHBOARD_READ, DASHBOARD_READ)
    );

    private AccessPolicy() {
    }

    public static String pathOf(HttpServletRequest request) {
        String info = request.getPathInfo();
        return request.getServletPath() + (info == null ? "" : info);
    }

    public static boolean isPublic(String path) {
        return path.equals("/login")
                || path.startsWith("/login/")
                || path.equals("/health")
                || path.startsWith("/assets/");
    }

    public static boolean isAjax(String path) {
        return path.startsWith("/speaker/poll") || path.startsWith("/orders/status");
    }

    public static Permission requiredFor(String path, String method) {
        boolean readOnly = "GET".equals(method) || "HEAD".equals(method);
        for (Rule rule : RULES) {
            if (path.equals(rule.prefix) || path.startsWith(rule.prefix + "/")) {
                return readOnly ? rule.read : rule.write;
            }
        }
        // Fail closed: a path nobody listed above is for admins only, until it gets its own rule
        return isOpenToSignedIn(path) ? null : SYSTEM_ADMIN;
    }

    // Pages every signed-in user may open whatever the role
    private static boolean isOpenToSignedIn(String path) {
        return path.equals("/") || path.equals("/index.html") || path.equals("/profile") || path.equals("/logout");
    }
}
