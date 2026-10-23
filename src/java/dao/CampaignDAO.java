package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Campaign;
import util.DBUtil;

public class CampaignDAO {

    private static final String SELECT = "SELECT c.campaign_id, c.name, c.description, c.commission_type, c.commission_value, "
            + "c.min_order, c.start_date, c.end_date, c.status, c.created_at, "
            + "(SELECT COUNT(*) FROM Orders o WHERE o.referral_campaign_id = c.campaign_id AND o.status IN (N'COMPLETED', N'PAID')) AS order_count, "
            + "ISNULL((SELECT SUM(o.amount) FROM Orders o WHERE o.referral_campaign_id = c.campaign_id AND o.status IN (N'COMPLETED', N'PAID')), 0) AS revenue "
            + "FROM Campaigns c ";

    public List<Campaign> search(String q, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT).append("WHERE 1 = 1 ");
        List<String> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND c.name LIKE ? ");
            args.add("%" + q + "%");
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND c.status = ? ");
            args.add(status);
        }
        sql.append("ORDER BY c.created_at DESC, c.campaign_id DESC");
        List<Campaign> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Campaign find(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE c.campaign_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Campaign> findRunning() throws SQLException {
        String sql = SELECT + "WHERE c.status = N'ACTIVE' AND CAST(GETDATE() AS DATE) BETWEEN c.start_date AND c.end_date";
        List<Campaign> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public void insert(Campaign c, int userId) throws SQLException {
        String sql = "INSERT INTO Campaigns (name, description, commission_type, commission_value, min_order, start_date, end_date, status, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, c);
            ps.setString(8, c.getStatus());
            ps.setInt(9, userId);
            ps.executeUpdate();
        }
    }

    public boolean update(Campaign c) throws SQLException {
        String sql = "UPDATE Campaigns SET name = ?, description = ?, commission_type = ?, commission_value = ?, "
                + "min_order = ?, start_date = ?, end_date = ? WHERE campaign_id = ? AND status <> N'ENDED'";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, c);
            ps.setInt(8, c.getCampaignId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setStatus(int id, String status, String... allowedFrom) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE Campaigns SET status = ? WHERE campaign_id = ? AND status IN (");
        for (int i = 0; i < allowedFrom.length; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        sql.append(")");
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            ps.setString(1, status);
            ps.setInt(2, id);
            for (int i = 0; i < allowedFrom.length; i++) {
                ps.setString(3 + i, allowedFrom[i]);
            }
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteUnused(int id) throws SQLException {
        String sql = "DELETE FROM Campaigns WHERE campaign_id = ? AND NOT EXISTS (SELECT 1 FROM Orders WHERE referral_campaign_id = ?) "
                + "AND NOT EXISTS (SELECT 1 FROM Commissions WHERE campaign_id = ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static void bind(PreparedStatement ps, Campaign c) throws SQLException {
        ps.setString(1, c.getName());
        ps.setString(2, c.getDescription() == null || c.getDescription().isEmpty() ? null : c.getDescription());
        ps.setString(3, c.getCommissionType());
        ps.setBigDecimal(4, c.getCommissionValue());
        ps.setBigDecimal(5, c.getMinOrder());
        ps.setDate(6, c.getStartDate());
        ps.setDate(7, c.getEndDate());
    }

    private Campaign map(ResultSet rs) throws SQLException {
        Campaign c = new Campaign();
        c.setCampaignId(rs.getInt("campaign_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setCommissionType(rs.getString("commission_type"));
        c.setCommissionValue(rs.getBigDecimal("commission_value"));
        c.setMinOrder(rs.getBigDecimal("min_order"));
        c.setStartDate(rs.getDate("start_date"));
        c.setEndDate(rs.getDate("end_date"));
        c.setStatus(rs.getString("status"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        c.setOrderCount(rs.getInt("order_count"));
        c.setRevenue(rs.getBigDecimal("revenue"));
        return c;
    }
}
