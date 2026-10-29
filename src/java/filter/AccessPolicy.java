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
            new Rule("/admin/categories", PRODUCT_MANAGE, PRODUCT_MANAGE),
            new Rule("/admin/groups", PRODUCT_MANAGE, PRODUCT_MANAGE),
            new Rule("/admin/stock-history", PRODUCT_MANAGE, PRODUCT_MANAGE),
            new Rule("/admin/audit", AUDIT_READ, AUDIT_READ),
            new Rule("/admin/taxes", TAX_READ, TAX_MANAGE),
            new Rule("/admin/affiliates", SALES_MANAGE, SALES_MANAGE),
            new Rule("/admin/affiliate-overview", SALES_MANAGE, SALES_MANAGE),
            new Rule("/admin/campaigns", SALES_MANAGE, SALES_MANAGE),
            new Rule("/admin/referrals", SALES_MANAGE, SALES_MANAGE),
            new Rule("/admin/commissions", SALES_MANAGE, SALES_MANAGE),
            new Rule("/admin/payouts", SALES_MANAGE, SALES_MANAGE),
            new Rule("/admin/channel-commissions", AFFILIATE_FINANCE_READ, SALES_MANAGE),
            new Rule("/admin/channel-payouts", AFFILIATE_FINANCE_READ, SALES_MANAGE),
            new Rule("/admin/channel-reports", AFFILIATE_FINANCE_READ, SALES_MANAGE),
            new Rule("/affiliate", AFFILIATE_PORTAL, AFFILIATE_PORTAL),
            new Rule("/admin", SYSTEM_ADMIN, SYSTEM_ADMIN),
            new Rule("/shift/print", SHIFT_PRINT, SHIFT_PRINT),
            new Rule("/shift", SHIFT_USE, SHIFT_USE),
            new Rule("/reports/shifts", INVOICE_READ, INVOICE_READ),
            new Rule("/invoices/manage", SALES_MANAGE, SALES_MANAGE),
            new Rule("/invoices/new", SALES_MANAGE, SALES_MANAGE),
            new Rule("/invoices/export", SALES_MANAGE, SALES_MANAGE),
            new Rule("/invoices", INVOICE_READ, INVOICE_READ),
            new Rule("/sales", SALES_MANAGE, SALES_MANAGE),
            new Rule("/analytics", SALES_MANAGE, SALES_MANAGE),
            new Rule("/orders", ORDER_CREATE, ORDER_CREATE),
            new Rule("/speaker", SPEAKER_USE, SPEAKER_USE),
            new Rule("/printer", SPEAKER_USE, SPEAKER_USE),
            new Rule("/transactions/review", TRANSACTION_RESOLVE, TRANSACTION_RESOLVE),
            new Rule("/transactions", TRANSACTION_READ, TRANSACTION_READ),
            new Rule("/alerts", ALERT_READ, ALERT_WRITE),
            new Rule("/tax/profile", TAX_READ, TAX_MANAGE),
            new Rule("/tax/calculator", TAX_READ, TAX_MANAGE),
            new Rule("/tax/obligations", TAX_READ, TAX_MANAGE),
            new Rule("/tax/payments", TAX_READ, TAX_MANAGE),
            new Rule("/tax/expenses", TAX_READ, TAX_MANAGE),
            new Rule("/tax/reconciliation", TAX_READ, TAX_MANAGE),
            new Rule("/tax/checker", TAX_READ, TAX_MANAGE),
            new Rule("/tax/reports", TAX_READ, TAX_MANAGE),
            new Rule("/tax/calendar", TAX_READ, TAX_MANAGE),
            new Rule("/tax/documents", TAX_READ, TAX_MANAGE),
            new Rule("/tax/assistant", TAX_READ, TAX_MANAGE),
            new Rule("/tax/notifications", TAX_READ, TAX_READ),
            new Rule("/tax", TAX_READ, TAX_READ),
            new Rule("/bot/chat", DASHBOARD_READ, DASHBOARD_READ),
            new Rule("/bot", TAX_READ, TAX_READ),
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
        return path.startsWith("/speaker/poll") || path.startsWith("/orders/status") || path.startsWith("/bot/feed") || path.startsWith("/bot/chat") || path.startsWith("/guide/progress");
    }

    public static Permission requiredFor(String path, String method) {
        boolean readOnly = "GET".equals(method) || "HEAD".equals(method);
        for (Rule rule : RULES) {
            if (path.equals(rule.prefix) || path.startsWith(rule.prefix + "/")) {
                return readOnly ? rule.read : rule.write;
            }
        }
        return isOpenToSignedIn(path) ? null : SYSTEM_ADMIN;
    }

    private static boolean isOpenToSignedIn(String path) {
        return path.equals("/") || path.equals("/index.html") || path.equals("/profile") || path.equals("/guide") || path.startsWith("/guide/") || path.equals("/logout");
    }
}
