package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Attribution;
import model.Commission;
import util.DBUtil;

public class CommissionDAO {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String ON_HOLD = "ON_HOLD";
    public static final String REJECTED = "REJECTED";
    public static final String PAID = "PAID";
    public static final String REVERSED = "REVERSED";
    public static final String[] ALL_STATUSES = {PENDING, APPROVED, AVAILABLE, ON_HOLD, REJECTED, PAID, REVERSED};

    private static final String ORDER_DONE = "EXISTS (SELECT 1 FROM Orders o WHERE o.order_id = Commissions.order_id AND o.status IN (N'COMPLETED', N'PAID'))";

    private static final String SELECT = "SELECT c.commission_id, c.affiliate_id, a.name AS affiliate_name, c.order_id, o.order_code, c.campaign_id, k.name AS campaign_name, "
            + "c.order_amount, c.eligible_amount, c.rate_text, c.original_amount, c.amount, c.status, c.rule_snapshot, c.reason, c.created_at "
            + "FROM Commissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id JOIN Orders o ON o.order_id = c.order_id JOIN Campaigns k ON k.campaign_id = c.campaign_id ";

    public static void insert(Connection con, int orderId, long orderAmount, Attribution at) throws SQLException {
        String sql = "INSERT INTO Commissions (affiliate_id, order_id, campaign_id, order_amount, eligible_amount, rate_text, original_amount, amount, status, rule_snapshot) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, N'PENDING', ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, at.getAffiliate().getAffiliateId());
            ps.setInt(2, orderId);
            ps.setInt(3, at.getCampaign().getCampaignId());
            ps.setBigDecimal(4, BigDecimal.valueOf(orderAmount));
            ps.setBigDecimal(5, BigDecimal.valueOf(at.getEligibleAmount()));
            ps.setString(6, at.getRateText());
            ps.setBigDecimal(7, BigDecimal.valueOf(at.getCommission()));
            ps.setBigDecimal(8, BigDecimal.valueOf(at.getCommission()));
            ps.setString(9, at.getSnapshot());
            ps.executeUpdate();
        }
    }

    public List<Commission> search(String q, String status, Integer affiliateId, int days) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND (o.order_code LIKE ? OR a.name LIKE ?) ");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND c.status = ? ");
            args.add(status);
        }
        if (affiliateId != null) {
            sql.append("AND c.affiliate_id = ? ");
            args.add(affiliateId);
        }
        if (days > 0) {
            sql.append("AND c.created_at >= DATEADD(DAY, ?, GETDATE()) ");
            args.add(-days);
        }
        sql.append("ORDER BY c.created_at DESC, c.commission_id DESC");
        List<Commission> list = new ArrayList<>();
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

    public Commission find(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE c.commission_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Map<String, BigDecimal> totalsByStatus(Integer affiliateId) throws SQLException {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String s : ALL_STATUSES) {
            totals.put(s, BigDecimal.ZERO);
        }
        String sql = "SELECT status, SUM(CASE WHEN status = N'REVERSED' THEN original_amount ELSE amount END) AS total FROM Commissions "
                + (affiliateId == null ? "" : "WHERE affiliate_id = ? ") + "GROUP BY status";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (affiliateId != null) {
                ps.setInt(1, affiliateId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    totals.put(rs.getString("status"), rs.getBigDecimal("total"));
                }
            }
        }
        return totals;
    }

    public boolean approve(int id, int payableDays) throws SQLException {
        String sql = "UPDATE Commissions SET status = CASE WHEN created_at <= DATEADD(DAY, ?, SYSDATETIME()) THEN N'AVAILABLE' ELSE N'APPROVED' END, "
                + "reason = NULL, updated_at = SYSDATETIME() WHERE commission_id = ? AND status IN (N'PENDING', N'ON_HOLD') AND " + ORDER_DONE;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -payableDays);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean reject(int id, String reason) throws SQLException {
        String sql = "UPDATE Commissions SET status = N'REJECTED', amount = 0, reason = ?, updated_at = SYSDATETIME() "
                + "WHERE commission_id = ? AND status IN (N'PENDING', N'APPROVED', N'ON_HOLD', N'AVAILABLE')";
        return exec(sql, reason, id);
    }

    public boolean hold(int id, String reason) throws SQLException {
        String sql = "UPDATE Commissions SET status = N'ON_HOLD', reason = ?, updated_at = SYSDATETIME() WHERE commission_id = ? AND status IN (N'PENDING', N'APPROVED')";
        return exec(sql, reason, id);
    }

    public int bulkApprove(int reviewDays, int payableDays) throws SQLException {
        String sql = "UPDATE Commissions SET status = CASE WHEN created_at <= DATEADD(DAY, ?, SYSDATETIME()) THEN N'AVAILABLE' ELSE N'APPROVED' END, updated_at = SYSDATETIME() "
                + "WHERE status = N'PENDING' AND created_at <= DATEADD(DAY, ?, SYSDATETIME()) AND " + ORDER_DONE;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -payableDays);
            ps.setInt(2, -reviewDays);
            return ps.executeUpdate();
        }
    }

    public int bulkApprovableCount(int reviewDays) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Commissions WHERE status = N'PENDING' AND created_at <= DATEADD(DAY, ?, SYSDATETIME()) AND " + ORDER_DONE;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -reviewDays);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void releaseMatured(int payableDays) throws SQLException {
        String sql = "UPDATE Commissions SET status = N'AVAILABLE', updated_at = SYSDATETIME() WHERE status = N'APPROVED' AND created_at <= DATEADD(DAY, ?, SYSDATETIME())";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -payableDays);
            ps.executeUpdate();
        }
    }

    public boolean adjust(int id, long delta, String reason, int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                Commission c = null;
                try (PreparedStatement ps = con.prepareStatement("SELECT affiliate_id, amount, status FROM Commissions WITH (UPDLOCK) WHERE commission_id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            c = new Commission();
                            c.setAffiliateId(rs.getInt("affiliate_id"));
                            c.setAmount(rs.getBigDecimal("amount"));
                            c.setStatus(rs.getString("status"));
                        }
                    }
                }
                if (c == null || delta == 0 || REJECTED.equals(c.getStatus()) || REVERSED.equals(c.getStatus())) {
                    con.rollback();
                    return false;
                }
                if (PAID.equals(c.getStatus())) {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO CommissionAdjustments (commission_id, affiliate_id, amount, reason, created_by) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setInt(1, id);
                        ps.setInt(2, c.getAffiliateId());
                        ps.setBigDecimal(3, BigDecimal.valueOf(delta));
                        ps.setString(4, reason);
                        ps.setInt(5, userId);
                        ps.executeUpdate();
                    }
                } else {
                    long next = c.getAmount().longValue() + delta;
                    if (next < 0) {
                        con.rollback();
                        return false;
                    }
                    try (PreparedStatement ps = con.prepareStatement("UPDATE Commissions SET amount = ?, status = CASE WHEN ? = 0 THEN N'REVERSED' ELSE status END, reason = ?, updated_at = SYSDATETIME() WHERE commission_id = ?")) {
                        ps.setBigDecimal(1, BigDecimal.valueOf(next));
                        ps.setLong(2, next);
                        ps.setString(3, reason);
                        ps.setInt(4, id);
                        ps.executeUpdate();
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

    private boolean exec(String sql, String reason, int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    private Commission map(ResultSet rs) throws SQLException {
        Commission c = new Commission();
        c.setCommissionId(rs.getInt("commission_id"));
        c.setAffiliateId(rs.getInt("affiliate_id"));
        c.setAffiliateName(rs.getString("affiliate_name"));
        c.setOrderId(rs.getInt("order_id"));
        c.setOrderCode(rs.getString("order_code"));
        c.setCampaignId(rs.getInt("campaign_id"));
        c.setCampaignName(rs.getString("campaign_name"));
        c.setOrderAmount(rs.getBigDecimal("order_amount"));
        c.setEligibleAmount(rs.getBigDecimal("eligible_amount"));
        c.setRateText(rs.getString("rate_text"));
        c.setOriginalAmount(rs.getBigDecimal("original_amount"));
        c.setAmount(rs.getBigDecimal("amount"));
        c.setStatus(rs.getString("status"));
        c.setRuleSnapshot(rs.getString("rule_snapshot"));
        c.setReason(rs.getString("reason"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}
