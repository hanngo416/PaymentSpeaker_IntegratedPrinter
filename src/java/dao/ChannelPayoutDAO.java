package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.ChannelCommission;
import model.ChannelPayout;
import util.DBUtil;

public class ChannelPayoutDAO {

    private static final String SELECT
            = "SELECT p.payout_id, p.reference, p.affiliate_id, a.name AS affiliate_name, p.total_amount, p.payment_method, "
            + "p.payment_reference, p.payment_date, p.status, p.notes, cu.full_name AS created_by_name, p.created_at, "
            + "fu.full_name AS confirmed_by_name, p.confirmed_at, "
            + "(SELECT COUNT(*) FROM ChannelPayoutItems i WHERE i.payout_id = p.payout_id) AS item_count "
            + "FROM ChannelPayouts p JOIN Affiliates a ON a.affiliate_id = p.affiliate_id "
            + "JOIN Users cu ON cu.user_id = p.created_by LEFT JOIN Users fu ON fu.user_id = p.confirmed_by ";

    private final ChannelCommissionDAO commissionDAO = new ChannelCommissionDAO();

    public List<ChannelCommission> outstanding() throws SQLException {
        return commissionDAO.search("", "", null, "APPROVED", null, null).stream()
                .filter(c -> c.getPayoutId() == null)
                .sorted((x, y) -> x.getAffiliateName().compareToIgnoreCase(y.getAffiliateName()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ChannelPayout> search(String q, String status, Integer affiliateId) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND (a.name LIKE ? OR p.reference LIKE ? OR p.payment_reference LIKE ?) ");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND p.status = ? ");
            args.add(status);
        }
        if (affiliateId != null) {
            sql.append("AND p.affiliate_id = ? ");
            args.add(affiliateId);
        }
        sql.append("ORDER BY CASE WHEN p.status = N'DRAFT' THEN 0 ELSE 1 END, p.payout_id DESC");
        List<ChannelPayout> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public ChannelPayout find(int id) throws SQLException {
        ChannelPayout p = null;
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(SELECT + "WHERE p.payout_id = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        p = map(rs);
                    }
                }
            }
            if (p != null) {
                List<ChannelCommission> items = new ArrayList<>();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT i.commission_id, i.amount, c.channel, c.platform_order_id, c.order_date, c.base_amount "
                        + "FROM ChannelPayoutItems i JOIN ChannelCommissions c ON c.commission_id = i.commission_id "
                        + "WHERE i.payout_id = ? ORDER BY c.order_date, i.commission_id")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ChannelCommission c = new ChannelCommission();
                            c.setCommissionId(rs.getInt("commission_id"));
                            c.setApprovedAmount(rs.getBigDecimal("amount"));
                            c.setChannel(rs.getString("channel"));
                            c.setPlatformOrderId(rs.getString("platform_order_id"));
                            c.setOrderDate(rs.getDate("order_date"));
                            c.setBaseAmount(rs.getBigDecimal("base_amount"));
                            items.add(c);
                        }
                    }
                }
                p.setItems(items);
            }
        }
        return p;
    }

    public int createDraft(int affiliateId, List<Integer> commissionIds, String method, String notes, int userId) throws SQLException {
        if (commissionIds.isEmpty()) {
            return 0;
        }
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                int payoutId;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO ChannelPayouts (reference, affiliate_id, total_amount, payment_method, status, notes, created_by) "
                        + "VALUES (LEFT(REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N''), 20), ?, 0, ?, N'DRAFT', ?, ?)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, affiliateId);
                    ps.setString(2, method);
                    ps.setString(3, notes);
                    ps.setInt(4, userId);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        payoutId = keys.getInt(1);
                    }
                }
                for (int cid : commissionIds) {
                    int n;
                    try (PreparedStatement ps = con.prepareStatement(
                            "UPDATE ChannelCommissions SET payout_id = ? WHERE commission_id = ? AND affiliate_id = ? "
                            + "AND status = N'APPROVED' AND payout_id IS NULL AND approved_amount > 0")) {
                        ps.setInt(1, payoutId);
                        ps.setInt(2, cid);
                        ps.setInt(3, affiliateId);
                        n = ps.executeUpdate();
                    }
                    if (n != 1) {
                        con.rollback();
                        return 0;
                    }
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO ChannelPayoutItems (payout_id, commission_id, amount) "
                            + "SELECT ?, commission_id, approved_amount FROM ChannelCommissions WHERE commission_id = ?")) {
                        ps.setInt(1, payoutId);
                        ps.setInt(2, cid);
                        ps.executeUpdate();
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ChannelPayouts SET total_amount = (SELECT SUM(amount) FROM ChannelPayoutItems WHERE payout_id = ?), "
                        + "reference = N'CP-' + RIGHT(N'0000' + CAST(payout_id AS NVARCHAR(10)), 4) WHERE payout_id = ?")) {
                    ps.setInt(1, payoutId);
                    ps.setInt(2, payoutId);
                    ps.executeUpdate();
                }
                con.commit();
                return payoutId;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public boolean removeItem(int payoutId, int commissionId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                int left;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COUNT(*) FROM ChannelPayouts p JOIN ChannelPayoutItems i ON i.payout_id = p.payout_id "
                        + "WHERE p.payout_id = ? AND p.status = N'DRAFT'")) {
                    ps.setInt(1, payoutId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        left = rs.getInt(1);
                    }
                }
                if (left < 2) {
                    con.rollback();
                    return false;
                }
                int n;
                try (PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM ChannelPayoutItems WHERE payout_id = ? AND commission_id = ?")) {
                    ps.setInt(1, payoutId);
                    ps.setInt(2, commissionId);
                    n = ps.executeUpdate();
                }
                if (n != 1) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ChannelCommissions SET payout_id = NULL WHERE commission_id = ? AND payout_id = ? AND status = N'APPROVED'")) {
                    ps.setInt(1, commissionId);
                    ps.setInt(2, payoutId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ChannelPayouts SET total_amount = (SELECT SUM(amount) FROM ChannelPayoutItems WHERE payout_id = ?) WHERE payout_id = ?")) {
                    ps.setInt(1, payoutId);
                    ps.setInt(2, payoutId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public boolean confirm(int payoutId, String method, String paymentReference, Date paymentDate, String notes, int userId)
            throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal total;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT total_amount FROM ChannelPayouts WITH (UPDLOCK, HOLDLOCK) WHERE payout_id = ? AND status = N'DRAFT'")) {
                    ps.setInt(1, payoutId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        total = rs.getBigDecimal(1);
                    }
                }
                BigDecimal itemsSum;
                int items;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT ISNULL(SUM(amount), 0), COUNT(*) FROM ChannelPayoutItems WHERE payout_id = ?")) {
                    ps.setInt(1, payoutId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        itemsSum = rs.getBigDecimal(1);
                        items = rs.getInt(2);
                    }
                }
                if (items == 0 || total.compareTo(itemsSum) != 0) {
                    con.rollback();
                    return false;
                }
                int paid;
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE c SET status = N'PAID' FROM ChannelCommissions c "
                        + "JOIN ChannelPayoutItems i ON i.commission_id = c.commission_id AND i.payout_id = ? "
                        + "WHERE c.payout_id = ? AND c.status = N'APPROVED' AND c.approved_amount = i.amount")) {
                    ps.setInt(1, payoutId);
                    ps.setInt(2, payoutId);
                    paid = ps.executeUpdate();
                }
                if (paid != items) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ChannelPayouts SET status = N'CONFIRMED', payment_method = ?, payment_reference = ?, payment_date = ?, "
                        + "notes = ?, confirmed_by = ?, confirmed_at = SYSDATETIME() WHERE payout_id = ? AND status = N'DRAFT'")) {
                    ps.setString(1, method);
                    ps.setString(2, paymentReference);
                    ps.setDate(3, paymentDate);
                    ps.setString(4, notes);
                    ps.setInt(5, userId);
                    ps.setInt(6, payoutId);
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;
                    }
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public boolean cancel(int payoutId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                int n;
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ChannelPayouts SET status = N'CANCELLED' WHERE payout_id = ? AND status = N'DRAFT'")) {
                    ps.setInt(1, payoutId);
                    n = ps.executeUpdate();
                }
                if (n != 1) {
                    con.rollback();
                    return false;
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE ChannelCommissions SET payout_id = NULL WHERE payout_id = ? AND status = N'APPROVED'")) {
                    ps.setInt(1, payoutId);
                    ps.executeUpdate();
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private ChannelPayout map(ResultSet rs) throws SQLException {
        ChannelPayout p = new ChannelPayout();
        p.setPayoutId(rs.getInt("payout_id"));
        p.setReference(rs.getString("reference"));
        p.setAffiliateId(rs.getInt("affiliate_id"));
        p.setAffiliateName(rs.getString("affiliate_name"));
        p.setTotalAmount(rs.getBigDecimal("total_amount"));
        p.setPaymentMethod(rs.getString("payment_method"));
        p.setPaymentReference(rs.getString("payment_reference"));
        p.setPaymentDate(rs.getDate("payment_date"));
        p.setStatus(rs.getString("status"));
        p.setNotes(rs.getString("notes"));
        p.setCreatedByName(rs.getString("created_by_name"));
        p.setCreatedAt(rs.getTimestamp("created_at"));
        p.setConfirmedByName(rs.getString("confirmed_by_name"));
        p.setConfirmedAt(rs.getTimestamp("confirmed_at"));
        p.setItemCount(rs.getInt("item_count"));
        return p;
    }
}
