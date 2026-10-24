package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import model.ChannelCommission;
import util.DBUtil;

public class ChannelCommissionDAO {

    private static final String SELECT
            = "SELECT c.commission_id, c.affiliate_id, a.name AS affiliate_name, c.channel, c.external_ref, c.platform_order_id, "
            + "c.order_date, c.base_amount, c.commission_type, c.commission_rate, c.calculated_amount, c.reported_amount, "
            + "c.approved_amount, c.status, c.data_source, c.attribution, c.notes, c.reject_reason, c.payout_id, "
            + "p.reference AS payout_reference, c.created_by, c.created_at, u.full_name AS approved_by_name, c.approved_at "
            + "FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
            + "LEFT JOIN ChannelPayouts p ON p.payout_id = c.payout_id "
            + "LEFT JOIN Users u ON u.user_id = c.approved_by ";

    public List<ChannelCommission> search(String q, String channel, Integer affiliateId, String status, Date from, Date to)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND (c.platform_order_id LIKE ? OR c.external_ref LIKE ? OR a.name LIKE ?) ");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (channel != null && !channel.isEmpty()) {
            sql.append("AND c.channel = ? ");
            args.add(channel);
        }
        if (affiliateId != null) {
            sql.append("AND c.affiliate_id = ? ");
            args.add(affiliateId);
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND c.status = ? ");
            args.add(status);
        }
        if (from != null) {
            sql.append("AND c.order_date >= ? ");
            args.add(from);
        }
        if (to != null) {
            sql.append("AND c.order_date <= ? ");
            args.add(to);
        }
        sql.append("ORDER BY CASE WHEN c.status = N'PENDING' THEN 0 ELSE 1 END, c.order_date DESC, c.commission_id DESC");
        List<ChannelCommission> list = new ArrayList<>();
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

    public ChannelCommission find(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE c.commission_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Map<String, BigDecimal> totalsByStatus() throws SQLException {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String s : new String[]{"PENDING", "APPROVED", "PAID", "REJECTED", "CANCELLED"}) {
            totals.put(s, BigDecimal.ZERO);
        }
        String sql = "SELECT status, SUM(CASE WHEN status IN (N'PENDING') THEN ISNULL(reported_amount, calculated_amount) "
                + "ELSE ISNULL(approved_amount, ISNULL(reported_amount, calculated_amount)) END) AS total "
                + "FROM ChannelCommissions GROUP BY status";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                totals.put(rs.getString("status"), rs.getBigDecimal("total"));
            }
        }
        return totals;
    }

    public Set<String> existingRefs(String channel) throws SQLException {
        Set<String> refs = new HashSet<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT external_ref FROM ChannelCommissions WHERE channel = ?")) {
            ps.setString(1, channel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    refs.add(rs.getString(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return refs;
    }

    public boolean existsByRef(String channel, String ref) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT 1 FROM ChannelCommissions WHERE channel = ? AND external_ref = ?")) {
            ps.setString(1, channel);
            ps.setString(2, ref);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existsByRef(Connection con, String channel, String ref) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM ChannelCommissions WITH (UPDLOCK, HOLDLOCK) WHERE channel = ? AND external_ref = ?")) {
            ps.setString(1, channel);
            ps.setString(2, ref);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insert(ChannelCommission c) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return insert(con, c);
        }
    }

    public int insert(Connection con, ChannelCommission c) throws SQLException {
        String sql = "INSERT INTO ChannelCommissions (affiliate_id, channel, external_ref, platform_order_id, order_date, base_amount, "
                + "commission_type, commission_rate, calculated_amount, reported_amount, status, data_source, attribution, notes, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, N'PENDING', ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getAffiliateId());
            ps.setString(2, c.getChannel());
            ps.setString(3, c.getExternalRef());
            ps.setString(4, c.getPlatformOrderId());
            ps.setDate(5, c.getOrderDate());
            ps.setBigDecimal(6, c.getBaseAmount());
            ps.setString(7, c.getCommissionType());
            ps.setBigDecimal(8, c.getCommissionRate());
            ps.setBigDecimal(9, c.getCalculatedAmount());
            if (c.getReportedAmount() == null) {
                ps.setNull(10, Types.DECIMAL);
            } else {
                ps.setBigDecimal(10, c.getReportedAmount());
            }
            ps.setString(11, c.getDataSource());
            ps.setString(12, c.getAttribution());
            ps.setString(13, c.getNotes());
            ps.setInt(14, c.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public boolean approve(int id, int userId) throws SQLException {
        String sql = "UPDATE c SET status = N'APPROVED', approved_amount = ISNULL(c.reported_amount, c.calculated_amount), "
                + "approved_by = ?, approved_at = SYSDATETIME() "
                + "FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                + "WHERE c.commission_id = ? AND c.status = N'PENDING' AND a.status = N'ACTIVE' "
                + "AND ISNULL(c.reported_amount, c.calculated_amount) > 0";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean reject(int id, int userId, String reason) throws SQLException {
        String sql = "UPDATE ChannelCommissions SET status = N'REJECTED', reject_reason = ?, approved_by = ?, approved_at = SYSDATETIME() "
                + "WHERE commission_id = ? AND status = N'PENDING'";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, userId);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean cancel(int id, int userId, String reason) throws SQLException {
        String sql = "UPDATE ChannelCommissions SET status = N'CANCELLED', reject_reason = ?, approved_by = ?, approved_at = SYSDATETIME() "
                + "WHERE commission_id = ? AND status = N'APPROVED' AND payout_id IS NULL";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, reason);
            ps.setInt(2, userId);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    private ChannelCommission map(ResultSet rs) throws SQLException {
        ChannelCommission c = new ChannelCommission();
        c.setCommissionId(rs.getInt("commission_id"));
        c.setAffiliateId(rs.getInt("affiliate_id"));
        c.setAffiliateName(rs.getString("affiliate_name"));
        c.setChannel(rs.getString("channel"));
        c.setExternalRef(rs.getString("external_ref"));
        c.setPlatformOrderId(rs.getString("platform_order_id"));
        c.setOrderDate(rs.getDate("order_date"));
        c.setBaseAmount(rs.getBigDecimal("base_amount"));
        c.setCommissionType(rs.getString("commission_type"));
        c.setCommissionRate(rs.getBigDecimal("commission_rate"));
        c.setCalculatedAmount(rs.getBigDecimal("calculated_amount"));
        c.setReportedAmount(rs.getBigDecimal("reported_amount"));
        c.setApprovedAmount(rs.getBigDecimal("approved_amount"));
        c.setStatus(rs.getString("status"));
        c.setDataSource(rs.getString("data_source"));
        c.setAttribution(rs.getString("attribution"));
        c.setNotes(rs.getString("notes"));
        c.setRejectReason(rs.getString("reject_reason"));
        int payout = rs.getInt("payout_id");
        c.setPayoutId(rs.wasNull() ? null : payout);
        c.setPayoutReference(rs.getString("payout_reference"));
        c.setCreatedBy(rs.getInt("created_by"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setApprovedByName(rs.getString("approved_by_name"));
        c.setApprovedAt(rs.getTimestamp("approved_at"));
        return c;
    }
}
