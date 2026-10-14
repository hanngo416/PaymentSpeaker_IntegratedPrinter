package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.view.RefundRow;
import util.DBUtil;

public class RefundDAO {

    private static final String SELECT
            = "SELECT r.refund_id, o.order_code, o.created_by_code, r.amount, r.reason, r.status, "
            + "u.user_code AS handled_by, r.note, r.created_at, r.done_at "
            + "FROM Refunds r JOIN Orders o ON o.order_id = r.order_id "
            + "LEFT JOIN Users u ON u.user_id = r.handled_by ";

    // status null = all; cashierId null = every cashier. Newest first, capped so the page stays light
    public List<RefundRow> search(String status, Integer cashierId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1");
        if (status != null) {
            sql.append(" AND r.status = ?");
        }
        if (cashierId != null) {
            sql.append(" AND o.created_by = ?");
        }
        sql.append(" ORDER BY r.created_at DESC, r.refund_id DESC OFFSET 0 ROWS FETCH NEXT 200 ROWS ONLY");
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            if (status != null) {
                ps.setString(i++, status);
            }
            if (cashierId != null) {
                ps.setInt(i, cashierId);
            }
            return read(ps);
        }
    }

    public List<RefundRow> findByOrder(int orderId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE r.order_id = ? ORDER BY r.created_at, r.refund_id")) {
            ps.setInt(1, orderId);
            return read(ps);
        }
    }

    // Marks one refund as sent back and closes the fraud alert of its transfer, in one transaction.
    // A cashier can only handle refunds of their own orders (cashierId), a manager or admin passes null.
    public boolean markDone(int refundId, int userId, String note, Integer cashierId) throws SQLException {
        String done = "UPDATE Refunds SET status = N'DONE', handled_by = ?, note = ?, done_at = SYSDATETIME() "
                + "WHERE refund_id = ? AND status = N'PENDING'"
                + (cashierId == null ? "" : " AND order_id IN (SELECT order_id FROM Orders WHERE created_by = ?)");
        String closeAlert = "UPDATE FraudAlerts SET status = N'RESOLVED', resolved_by = ?, resolved_notes = ? "
                + "WHERE status IN (N'NEW', N'IN_PROGRESS') "
                + "AND trans_id = (SELECT trans_id FROM Refunds WHERE refund_id = ?)";
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(done)) {
                    ps.setInt(1, userId);
                    ps.setString(2, note);
                    ps.setInt(3, refundId);
                    if (cashierId != null) {
                        ps.setInt(4, cashierId);
                    }
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(closeAlert)) {
                    ps.setInt(1, userId);
                    ps.setString(2, truncate("Refunded: " + note, 500));
                    ps.setInt(3, refundId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private static List<RefundRow> read(PreparedStatement ps) throws SQLException {
        List<RefundRow> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                RefundRow r = new RefundRow();
                r.setRefundId(rs.getInt("refund_id"));
                r.setOrderCode(rs.getString("order_code"));
                r.setCashierCode(rs.getString("created_by_code"));
                r.setAmount(rs.getBigDecimal("amount"));
                r.setReason(rs.getString("reason"));
                r.setStatus(rs.getString("status"));
                r.setHandledBy(rs.getString("handled_by"));
                r.setNote(rs.getString("note"));
                r.setCreatedAt(rs.getTimestamp("created_at"));
                r.setDoneAt(rs.getTimestamp("done_at"));
                list.add(r);
            }
        }
        return list;
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }
}
