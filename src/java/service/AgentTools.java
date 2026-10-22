package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dao.AgentDAO;
import dao.SummaryDAO;
import filter.RolePermissions;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.TaxRule;
import model.User;
import model.enums.Permission;
import model.enums.Role;
import model.view.Summary;
import model.view.TaxEstimate;

public class AgentTools {

    public static class Tool {

        final String name;
        final String description;
        final String[] params;
        final Permission[] anyOf;

        Tool(String name, String description, String[] params, Permission... anyOf) {
            this.name = name;
            this.description = description;
            this.params = params;
            this.anyOf = anyOf;
        }

        public String getName() {
            return name;
        }

        boolean allowedFor(Role role) {
            for (Permission p : anyOf) {
                if (RolePermissions.has(role, p)) {
                    return true;
                }
            }
            return false;
        }

        JsonObject declaration() {
            JsonObject d = new JsonObject();
            d.addProperty("name", name);
            d.addProperty("description", description);
            if (params.length > 0) {
                JsonObject props = new JsonObject();
                for (String p : params) {
                    JsonObject s = new JsonObject();
                    s.addProperty("type", "STRING");
                    props.add(p, s);
                }
                JsonObject schema = new JsonObject();
                schema.addProperty("type", "OBJECT");
                schema.add("properties", props);
                d.add("parameters", schema);
            }
            return d;
        }
    }

    private static final String DATES = "Dates are yyyy-MM-dd; from is inclusive, to is exclusive; default is today.";
    private static final String PERIOD = "period is one of today, week, month, year (default today).";

    private static final List<Tool> TOOLS = Arrays.asList(
            new Tool("search_orders", "List orders (max 20). Optional: from, to, status (PENDING, COMPLETED, CANCELLED), code. "
                    + DATES, new String[]{"from", "to", "status", "code"}, Permission.ORDER_CREATE, Permission.PRODUCT_MANAGE),
            new Tool("search_transactions", "List bank transactions (max 20). Optional: from, to, status (COMPLETED, CANCELLED). "
                    + DATES, new String[]{"from", "to", "status"}, Permission.TRANSACTION_READ),
            new Tool("list_alerts", "List fraud alerts (max 20). Optional: status (NEW, IN_PROGRESS, RESOLVED, DISMISSED), severity (LOW, MEDIUM, HIGH).",
                    new String[]{"status", "severity"}, Permission.ALERT_READ),
            new Tool("revenue_summary", "Revenue totals of completed orders and bank money received. " + PERIOD + " Or give from and to instead. " + DATES,
                    new String[]{"period", "from", "to"}, Permission.INVOICE_READ, Permission.TAX_READ),
            new Tool("top_products", "Best selling products by quantity (max 20). " + PERIOD + " Or give from and to instead. " + DATES,
                    new String[]{"period", "from", "to"}, Permission.INVOICE_READ, Permission.TAX_READ),
            new Tool("order_items", "Items of one order. Required: code (for example TINKY12345).",
                    new String[]{"code"}, Permission.ORDER_CREATE, Permission.PRODUCT_MANAGE),
            new Tool("search_products", "Find products with price, group and category (max 20). Optional: query (name, code, group or category text).",
                    new String[]{"query"}, Permission.ORDER_CREATE, Permission.PRODUCT_MANAGE),
            new Tool("list_discounts", "Discount codes with kind, value, validity and usage.",
                    new String[0], Permission.DISCOUNT_MANAGE),
            new Tool("list_users", "Users with role and status, no emails (max 20). Optional: query (code, name, role or status text).",
                    new String[]{"query"}, Permission.USER_READ),
            new Tool("list_devices", "Devices (printer) with status and last ping.",
                    new String[0], Permission.DEVICE_READ),
            new Tool("tax_estimate", "This year's revenue against the tax-free threshold, with VAT and personal income tax estimates.",
                    new String[0], Permission.TAX_READ),
            new Tool("tax_rules", "Tax rules in force with their legal source reference.",
                    new String[0], Permission.TAX_READ));

    private final AgentDAO dao = new AgentDAO();
    private final SummaryDAO summaryDAO = new SummaryDAO();
    private final TaxService taxService = new TaxService();

    public List<Tool> availableFor(Role role) {
        List<Tool> list = new ArrayList<>();
        for (Tool t : TOOLS) {
            if (t.allowedFor(role)) {
                list.add(t);
            }
        }
        return list;
    }

    public JsonArray declarationsFor(Role role) {
        JsonArray arr = new JsonArray();
        for (Tool t : availableFor(role)) {
            arr.add(t.declaration());
        }
        return arr;
    }

    public JsonObject run(User user, String name, JsonObject args) {
        Tool tool = null;
        for (Tool t : TOOLS) {
            if (t.name.equals(name)) {
                tool = t;
            }
        }
        if (tool == null) {
            return error("Unknown function " + name);
        }
        if (!tool.allowedFor(user.getRole())) {
            return error("Your role is not allowed to use " + name);
        }
        try {
            return execute(user, name, args);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        } catch (SQLException e) {
            return error("The data could not be read");
        }
    }

    private JsonObject execute(User user, String name, JsonObject args) throws SQLException {
        switch (name) {
            case "search_orders": {
                boolean seeAll = user.getRole() == Role.ADMIN || user.getRole() == Role.STORE_MANAGER;
                return dao.orders(from(args), to(args), choice(args, "status", "PENDING", "COMPLETED", "CANCELLED"),
                        text(args, "code"), seeAll ? null : Integer.valueOf(user.getUserId()));
            }
            case "search_transactions":
                return dao.transactions(from(args), to(args), choice(args, "status", "COMPLETED", "CANCELLED"));
            case "list_alerts":
                return dao.alerts(choice(args, "status", "NEW", "IN_PROGRESS", "RESOLVED", "DISMISSED"),
                        choice(args, "severity", "LOW", "MEDIUM", "HIGH"));
            case "revenue_summary": {
                Timestamp[] w = range(args);
                Summary s = summaryDAO.summarize(w[0], w[1]);
                JsonObject o = new JsonObject();
                o.addProperty("completed_orders", s.getOrderCount());
                o.addProperty("revenue_vnd", s.getTotal().toBigInteger().toString());
                o.addProperty("cash_vnd", s.getCash().toBigInteger().toString());
                o.addProperty("pending_orders", s.getPendingOrders());
                o.addProperty("bank_transactions", s.getBankCount());
                o.addProperty("bank_received_vnd", s.getBankReceived().toBigInteger().toString());
                return o;
            }
            case "order_items": {
                String code = text(args, "code");
                if (code == null) {
                    throw new IllegalArgumentException("code is required");
                }
                boolean seeAll = user.getRole() == Role.ADMIN || user.getRole() == Role.STORE_MANAGER;
                return dao.orderItems(code, seeAll ? null : Integer.valueOf(user.getUserId()));
            }
            case "search_products":
                return dao.products(text(args, "query"), user.getRole() == Role.CASHIER);
            case "list_discounts":
                return dao.discounts();
            case "list_users":
                return dao.users(text(args, "query"));
            case "list_devices":
                return dao.devices();
            case "top_products": {
                Timestamp[] w = range(args);
                return dao.topProducts(w[0], w[1]);
            }
            case "tax_estimate": {
                int year = LocalDate.now().getYear();
                TaxService.TaxView v = taxService.view(year);
                JsonObject o = new JsonObject();
                o.addProperty("year", year);
                TaxEstimate e = v.getEstimate();
                if (e == null) {
                    o.addProperty("note", "Tax rules incomplete, no estimate");
                    return o;
                }
                o.addProperty("revenue_vnd", e.getRevenue().toBigInteger().toString());
                o.addProperty("percent_of_threshold", e.getPercent().toPlainString());
                o.addProperty("status", e.getStatus());
                o.addProperty("remaining_vnd", e.getRemaining().toBigInteger().toString());
                o.addProperty("vat_estimate_vnd", e.getVat().toBigInteger().toString());
                o.addProperty("pit_estimate_vnd", e.getPit().toBigInteger().toString());
                o.addProperty("note", "Estimate only, not the amount to pay");
                return o;
            }
            default: {
                TaxService.TaxView v = taxService.view(LocalDate.now().getYear());
                JsonArray rows = new JsonArray();
                for (TaxRule r : v.getSources()) {
                    if (rows.size() == AgentDAO.MAX_ROWS) {
                        break;
                    }
                    JsonObject row = new JsonObject();
                    row.addProperty("kind", r.getKind());
                    row.addProperty("value", r.getValue().toPlainString());
                    row.addProperty("source", r.getSourceRef());
                    row.addProperty("verified", r.isVerified());
                    rows.add(row);
                }
                JsonObject o = new JsonObject();
                o.add("rows", rows);
                return o;
            }
        }
    }

    private static Timestamp[] range(JsonObject args) {
        if (text(args, "from") != null || text(args, "to") != null) {
            return new Timestamp[]{from(args), to(args)};
        }
        return window(text(args, "period"));
    }

    static Timestamp[] window(String period) {
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        String p = period == null ? "today" : period.toLowerCase();
        switch (p) {
            case "today":
                start = today;
                end = today.plusDays(1);
                break;
            case "week":
                start = today.with(DayOfWeek.MONDAY);
                end = start.plusDays(7);
                break;
            case "month":
                start = today.withDayOfMonth(1);
                end = start.plusMonths(1);
                break;
            case "year":
                start = today.withDayOfYear(1);
                end = start.plusYears(1);
                break;
            default:
                throw new IllegalArgumentException("period must be today, week, month or year");
        }
        return new Timestamp[]{Timestamp.valueOf(start.atStartOfDay()), Timestamp.valueOf(end.atStartOfDay())};
    }

    private static Timestamp from(JsonObject args) {
        String s = text(args, "from");
        LocalDate d = s == null ? LocalDate.now() : date(s);
        return Timestamp.valueOf(d.atStartOfDay());
    }

    private static Timestamp to(JsonObject args) {
        String s = text(args, "to");
        LocalDate d = s == null ? LocalDate.now().plusDays(1) : date(s);
        return Timestamp.valueOf(d.atStartOfDay());
    }

    private static LocalDate date(String s) {
        try {
            return LocalDate.parse(s);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must look like 2026-10-22");
        }
    }

    private static String text(JsonObject args, String key) {
        if (args == null || !args.has(key) || args.get(key).isJsonNull()) {
            return null;
        }
        String s = args.get(key).getAsString().trim();
        return s.isEmpty() ? null : (s.length() > 50 ? s.substring(0, 50) : s);
    }

    private static String choice(JsonObject args, String key, String... allowed) {
        String s = text(args, key);
        if (s == null) {
            return null;
        }
        for (String a : allowed) {
            if (a.equalsIgnoreCase(s)) {
                return a;
            }
        }
        throw new IllegalArgumentException(key + " must be one of " + Arrays.toString(allowed));
    }

    private static JsonObject error(String message) {
        JsonObject o = new JsonObject();
        o.addProperty("error", message);
        return o;
    }
}
