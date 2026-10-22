package dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import util.DBUtil;

public class AgentDAO {

    public static final int MAX_ROWS = 20;

    public JsonObject orders(Timestamp from, Timestamp to, String status, String code, Integer onlyUserId) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT TOP (21) order_code, created_by_code, amount, cash_amount, status, created_at FROM Orders WHERE created_at >= ? AND created_at < ? ");
        List<Object> args = new ArrayList<>();
        args.add(from);
        args.add(to);
        if (status != null) {
            sql.append("AND status = ? ");
            args.add(status);
        }
        if (code != null) {
            sql.append("AND order_code = ? ");
            args.add(code);
        }
        if (onlyUserId != null) {
            sql.append("AND created_by = ? ");
            args.add(onlyUserId);
        }
        sql.append("ORDER BY created_at DESC");
        return query(sql.toString(), args);
    }

    public JsonObject transactions(Timestamp from, Timestamp to, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT TOP (21) t.trans_id, t.amount, t.status, t.trans_time, o.order_code FROM Transactions t "
                + "LEFT JOIN Orders o ON o.order_id = t.order_id WHERE t.trans_time >= ? AND t.trans_time < ? ");
        List<Object> args = new ArrayList<>();
        args.add(from);
        args.add(to);
        if (status != null) {
            sql.append("AND t.status = ? ");
            args.add(status);
        }
        sql.append("ORDER BY t.trans_time DESC");
        return query(sql.toString(), args);
    }

    public JsonObject alerts(String status, String severity) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT TOP (21) alert_id, rule_code, severity, status, reason, created_at FROM FraudAlerts WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (status != null) {
            sql.append("AND status = ? ");
            args.add(status);
        }
        if (severity != null) {
            sql.append("AND severity = ? ");
            args.add(severity);
        }
        sql.append("ORDER BY created_at DESC");
        return query(sql.toString(), args);
    }

    public JsonObject topProducts(Timestamp from, Timestamp to) throws SQLException {
        List<Object> args = new ArrayList<>();
        args.add(from);
        args.add(to);
        return query("SELECT TOP (21) oi.product_name, SUM(oi.quantity) AS quantity, SUM(oi.subtotal) AS revenue "
                + "FROM OrderItems oi JOIN Orders o ON o.order_id = oi.order_id "
                + "WHERE o.status IN (N'COMPLETED', N'PAID') AND o.created_at >= ? AND o.created_at < ? "
                + "GROUP BY oi.product_name ORDER BY SUM(oi.quantity) DESC", args);
    }

    public JsonObject orderItems(String orderCode, Integer onlyUserId) throws SQLException {
        List<Object> args = new ArrayList<>();
        args.add(orderCode);
        String sql = "SELECT TOP (21) oi.product_name, oi.unit_price, oi.quantity, oi.subtotal FROM OrderItems oi "
                + "JOIN Orders o ON o.order_id = oi.order_id WHERE o.order_code = ? ";
        if (onlyUserId != null) {
            sql += "AND o.created_by = ? ";
            args.add(onlyUserId);
        }
        return query(sql + "ORDER BY oi.item_id", args);
    }

    public JsonObject products(String q, boolean activeOnly) throws SQLException {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT TOP (21) p.product_code, p.product_name, p.price, p.active, g.group_name, c.category_name "
                + "FROM Products p JOIN [Groups] g ON g.group_id = p.group_id JOIN Categories c ON c.category_id = g.category_id WHERE 1 = 1 ";
        if (q != null) {
            sql += "AND (p.product_name LIKE ? OR p.product_code LIKE ? OR g.group_name LIKE ? OR c.category_name LIKE ?) ";
            for (int i = 0; i < 4; i++) {
                args.add("%" + q + "%");
            }
        }
        if (activeOnly) {
            sql += "AND p.active = 1 ";
        }
        return query(sql + "ORDER BY c.display_order, g.display_order, p.display_order, p.product_name", args);
    }

    public JsonObject discounts() throws SQLException {
        return query("SELECT TOP (21) code, kind, value, max_uses, used_count, valid_from, valid_to, active, holder "
                + "FROM DiscountCodes ORDER BY created_at DESC", new ArrayList<Object>());
    }

    public JsonObject users(String q) throws SQLException {
        List<Object> args = new ArrayList<>();
        String sql = "SELECT TOP (21) u.user_code, u.full_name, r.role_name AS role, u.status FROM Users u "
                + "JOIN Roles r ON r.role_id = u.role_id WHERE 1 = 1 ";
        if (q != null) {
            sql += "AND (u.user_code LIKE ? OR u.full_name LIKE ? OR r.role_name LIKE ? OR u.status LIKE ?) ";
            for (int i = 0; i < 4; i++) {
                args.add("%" + q + "%");
            }
        }
        return query(sql + "ORDER BY u.user_code", args);
    }

    public JsonObject devices() throws SQLException {
        return query("SELECT TOP (21) device_code, device_type, model_name, status, last_ping FROM Devices ORDER BY device_code",
                new ArrayList<Object>());
    }

    private JsonObject query(String sql, List<Object> args) throws SQLException {
        JsonArray rows = new JsonArray();
        boolean truncated = false;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    if (rows.size() == MAX_ROWS) {
                        truncated = true;
                        break;
                    }
                    JsonObject row = new JsonObject();
                    for (int c = 1; c <= meta.getColumnCount(); c++) {
                        Object v = rs.getObject(c);
                        row.addProperty(meta.getColumnLabel(c), v == null ? "" : v.toString());
                    }
                    rows.add(row);
                }
            }
        }
        JsonObject result = new JsonObject();
        result.add("rows", rows);
        result.addProperty("truncated", truncated);
        return result;
    }
}
