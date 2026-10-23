package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.AlertFilter;
import model.Page;
import model.view.AlertRow;
import util.DBUtil;

public class FraudAlertDAO {

    private static final String FROM
            = " FROM FraudAlerts a "
            + "JOIN Transactions t ON t.trans_id = a.trans_id "
            + "LEFT JOIN Users u ON u.user_id = a.resolved_by";

    private static final String UPDATE_STATUS
            = "UPDATE FraudAlerts SET status = ?, resolved_by = ?, resolved_notes = ? "
            + "WHERE alert_id = ? AND status IN (N'NEW', N'IN_PROGRESS')";

    public Page<AlertRow> search(AlertFilter f) throws SQLException {
        List<Object> params = new ArrayList<>();
        String where = where(f, params);
        int total;
        List<AlertRow> items = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            String sql = "SELECT a.alert_id, a.rule_code, a.reason, a.severity, a.status, a.resolved_notes, "
                    + "a.created_at, u.user_code AS resolved_by, t.amount, t.content" + FROM + where
                    + " ORDER BY a.created_at DESC, a.alert_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, params);
                ps.setInt(params.size() + 1, f.getOffset());
                ps.setInt(params.size() + 2, f.getPageSize());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        AlertRow r = new AlertRow();
                        r.setAlertId(rs.getInt("alert_id"));
                        r.setRuleCode(rs.getString("rule_code"));
                        r.setReason(rs.getString("reason"));
                        r.setSeverity(rs.getString("severity"));
                        r.setStatus(rs.getString("status"));
                        r.setResolvedNotes(rs.getString("resolved_notes"));
                        r.setCreatedAt(rs.getTimestamp("created_at"));
                        r.setResolvedBy(rs.getString("resolved_by"));
                        r.setAmount(rs.getBigDecimal("amount"));
                        r.setContent(rs.getString("content"));
                        items.add(r);
                    }
                }
            }
        }
        return new Page<>(items, total, f.getPage(), f.getPageSize());
    }

    public int countNew() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM FraudAlerts WHERE status = N'NEW'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public Map<String, Integer> countByStatus() throws SQLException {
        String sql = "SELECT a.status, COUNT(*) AS cnt" + FROM
                + " GROUP BY a.status";
        Map<String, Integer> counts = new LinkedHashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getString("status"), rs.getInt("cnt"));
                }
            }
        }
        return counts;
    }

    public boolean updateStatus(int alertId, String status, int userId, String notes) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            ps.setString(3, notes);
            ps.setInt(4, alertId);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean startReview(int alertId, int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE FraudAlerts SET status = N'IN_PROGRESS', resolved_by = ? WHERE alert_id = ? AND status = N'NEW'")) {
            ps.setInt(1, userId);
            ps.setInt(2, alertId);
            return ps.executeUpdate() == 1;
        }
    }

    private static String where(AlertFilter f, List<Object> params) {
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");
        if (f.getStatus() != null) {
            w.append(" AND a.status = ?");
            params.add(f.getStatus());
        }
        if (f.getSeverity() != null) {
            w.append(" AND a.severity = ?");
            params.add(f.getSeverity());
        }
        return w.toString();
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}
