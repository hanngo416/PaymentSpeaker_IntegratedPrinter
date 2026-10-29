package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import model.Page;
import model.view.ReviewRow;
import util.DBUtil;
import util.SqlErrors;

public class TransactionReviewDAO {

    public static final String OK = "OK";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String ALREADY_RESOLVED = "ALREADY_RESOLVED";
    public static final String ORDER_NOT_PENDING = "ORDER_NOT_PENDING";
    public static final String AMOUNT_TOO_LOW = "AMOUNT_TOO_LOW";
    public static final String ORDER_ALREADY_LINKED = "ORDER_ALREADY_LINKED";

    public static final List<String> CATEGORIES = Arrays.asList(
            "REFUND_NEEDED", "WRONG_ACCOUNT_OR_SPAM", "DUPLICATE_TRANSFER", "OTHER");

    private static final String FROM
            = "FROM Transactions t "
            + "OUTER APPLY (SELECT TOP 1 a.rule_code, a.reason FROM FraudAlerts a WHERE a.trans_id = t.trans_id ORDER BY a.alert_id) x "
            + "LEFT JOIN Orders o ON o.order_id = t.order_id ";

    private static final String WHERE
            = "WHERE t.status = N'CANCELLED' AND t.resolution IS NULL "
            + "AND (? IS NULL OR x.rule_code = ?) AND (? IS NULL OR t.trans_time >= ?) "
            + "AND (? IS NULL OR t.trans_time < DATEADD(DAY, 1, ?)) ";

    public Page<ReviewRow> search(String ruleCode, Date from, Date to, int page, int pageSize) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) " + FROM + WHERE)) {
                bind(ps, ruleCode, from, to);
                total = count(ps);
            }
            List<ReviewRow> items = new ArrayList<>();
            String sql = "SELECT t.trans_id, t.bank_trans_id, t.amount, t.content, t.trans_time, x.rule_code, x.reason, o.order_code "
                    + FROM + WHERE + "ORDER BY t.trans_time DESC, t.trans_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int i = bind(ps, ruleCode, from, to);
                ps.setInt(i++, Math.max(0, (page - 1) * pageSize));
                ps.setInt(i, pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ReviewRow r = new ReviewRow();
                        r.setTransId(rs.getInt("trans_id"));
                        r.setBankTransId(rs.getString("bank_trans_id"));
                        r.setAmount(rs.getBigDecimal("amount"));
                        r.setContent(rs.getString("content"));
                        r.setTransTime(rs.getTimestamp("trans_time"));
                        r.setRuleCode(rs.getString("rule_code"));
                        r.setReason(rs.getString("reason"));
                        r.setOrderCode(rs.getString("order_code"));
                        items.add(r);
                    }
                }
            }
            return new Page<>(items, total, page, pageSize);
        }
    }

    public List<String> findRuleCodes() throws SQLException {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT DISTINCT x.rule_code " + FROM + "WHERE t.status = N'CANCELLED' AND t.resolution IS NULL AND x.rule_code IS NOT NULL ORDER BY x.rule_code";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                codes.add(rs.getString(1));
            }
        }
        return codes;
    }

    public String link(int transId, int orderId, int userId, String note) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal paid;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT amount, status, resolution FROM Transactions WITH (UPDLOCK) WHERE trans_id = ?")) {
                    ps.setInt(1, transId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return NOT_FOUND;
                        }
                        if (rs.getString("resolution") != null || !"CANCELLED".equals(rs.getString("status"))) {
                            con.rollback();
                            return ALREADY_RESOLVED;
                        }
                        paid = rs.getBigDecimal("amount");
                    }
                }
                BigDecimal due;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT amount - ISNULL(cash_amount, 0), status FROM Orders WITH (UPDLOCK) WHERE order_id = ?")) {
                    ps.setInt(1, orderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return NOT_FOUND;
                        }
                        if (!"PENDING".equals(rs.getString(2))) {
                            con.rollback();
                            return ORDER_NOT_PENDING;
                        }
                        due = rs.getBigDecimal(1);
                    }
                }
                if (paid.compareTo(due) < 0) {
                    con.rollback();
                    return AMOUNT_TOO_LOW;
                }
                String saved = paid.compareTo(due) > 0
                        ? note + " (excess " + paid.subtract(due).toBigInteger() + " VND)" : note;
                if (saved.length() > 200) {
                    saved = saved.substring(0, 200);
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE Orders SET status = N'COMPLETED' WHERE order_id = ? AND status = N'PENDING'")) {
                    ps.setInt(1, orderId);
                    if (ps.executeUpdate() == 0) {
                        con.rollback();
                        return ORDER_NOT_PENDING;
                    }
                }
                StockDAO.deductForOrder(con, orderId);
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Transactions SET status = N'COMPLETED', order_id = ?, linked_order_id = ?, resolution = N'LINKED', "
                        + "resolution_note = ?, resolved_by = ?, resolved_at = SYSDATETIME(), announced = 1 WHERE trans_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, orderId);
                    ps.setString(3, saved);
                    ps.setInt(4, userId);
                    ps.setInt(5, transId);
                    ps.executeUpdate();
                }
                resolveAlerts(con, transId, userId, "Linked to an order by review");
                con.commit();
                return OK;
            } catch (SQLException e) {
                con.rollback();
                if (SqlErrors.isDuplicateKey(e)) {
                    return ORDER_ALREADY_LINKED;
                }
                throw e;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public String close(int transId, String category, int userId, String note) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT status, resolution FROM Transactions WITH (UPDLOCK) WHERE trans_id = ?")) {
                    ps.setInt(1, transId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return NOT_FOUND;
                        }
                        if (rs.getString("resolution") != null || !"CANCELLED".equals(rs.getString("status"))) {
                            con.rollback();
                            return ALREADY_RESOLVED;
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE Transactions SET resolution = N'CLOSED', close_category = ?, resolution_note = ?, "
                        + "resolved_by = ?, resolved_at = SYSDATETIME() WHERE trans_id = ?")) {
                    ps.setString(1, category);
                    ps.setString(2, note);
                    ps.setInt(3, userId);
                    ps.setInt(4, transId);
                    ps.executeUpdate();
                }
                resolveAlerts(con, transId, userId, "Closed by review: " + category);
                con.commit();
                return OK;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private static void resolveAlerts(Connection con, int transId, int userId, String notes) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE FraudAlerts SET status = N'RESOLVED', resolved_by = ?, resolved_notes = ? "
                + "WHERE trans_id = ? AND status IN (N'NEW', N'IN_PROGRESS')")) {
            ps.setInt(1, userId);
            ps.setString(2, notes);
            ps.setInt(3, transId);
            ps.executeUpdate();
        }
    }

    private static int bind(PreparedStatement ps, String ruleCode, Date from, Date to) throws SQLException {
        int i = 1;
        ps.setString(i++, ruleCode);
        ps.setString(i++, ruleCode);
        ps.setDate(i++, from);
        ps.setDate(i++, from);
        ps.setDate(i++, to);
        ps.setDate(i++, to);
        return i;
    }

    private static int count(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public List<java.util.Map<String, Object>> findPendingOrders() throws SQLException {
        List<java.util.Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT TOP 100 order_id, order_code, amount - ISNULL(cash_amount, 0) AS transfer_amount, created_by_code, created_at "
                + "FROM Orders WHERE status = N'PENDING' ORDER BY created_at DESC";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("orderId", rs.getInt("order_id"));
                row.put("orderCode", rs.getString("order_code"));
                row.put("transferAmount", rs.getBigDecimal("transfer_amount"));
                row.put("createdByCode", rs.getString("created_by_code"));
                row.put("createdAt", rs.getTimestamp("created_at"));
                list.add(row);
            }
        }
        return list;
    }
}
