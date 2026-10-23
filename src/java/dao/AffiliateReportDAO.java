package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.DBUtil;

public class AffiliateReportDAO {

    private static final String DONE = "o.status IN (N'COMPLETED', N'PAID')";

    public Map<String, Object> kpis(int days) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM Affiliates) AS total, "
                + "(SELECT COUNT(*) FROM Affiliates WHERE status = N'ACTIVE') AS active, "
                + "(SELECT COUNT(*) FROM Affiliates WHERE status = N'PENDING') AS pending, "
                + "(SELECT COUNT(*) FROM Orders o WHERE o.referred_by_affiliate_id IS NOT NULL AND " + DONE + " AND o.created_at >= DATEADD(DAY, ?, GETDATE())) AS orders, "
                + "ISNULL((SELECT SUM(o.amount) FROM Orders o WHERE o.referred_by_affiliate_id IS NOT NULL AND " + DONE + " AND o.created_at >= DATEADD(DAY, ?, GETDATE())), 0) AS revenue, "
                + "ISNULL((SELECT SUM(c.amount) FROM Commissions c WHERE c.status NOT IN (N'REJECTED', N'REVERSED') AND c.created_at >= DATEADD(DAY, ?, GETDATE())), 0) AS commissions, "
                + "ISNULL((SELECT SUM(c.amount) FROM Commissions c WHERE c.status = N'AVAILABLE'), 0) "
                + "+ ISNULL((SELECT SUM(amount) FROM CommissionAdjustments WHERE payout_id IS NULL), 0) AS pending_payouts, "
                + "(SELECT COUNT(*) FROM Commissions WHERE status = N'PENDING') AS to_approve";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -days);
            ps.setInt(2, -days);
            ps.setInt(3, -days);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                m.put("total", rs.getInt("total"));
                m.put("active", rs.getInt("active"));
                m.put("pending", rs.getInt("pending"));
                m.put("orders", rs.getInt("orders"));
                m.put("revenue", rs.getBigDecimal("revenue"));
                m.put("commissions", rs.getBigDecimal("commissions"));
                m.put("pendingPayouts", rs.getBigDecimal("pending_payouts"));
                m.put("toApprove", rs.getInt("to_approve"));
            }
        }
        return m;
    }

    public List<Object[]> daily(int days) throws SQLException {
        String sql = "SELECT CAST(o.created_at AS DATE) AS d, COUNT(*) AS n, SUM(o.amount) AS revenue, ISNULL(SUM(c.amount), 0) AS commission "
                + "FROM Orders o LEFT JOIN Commissions c ON c.order_id = o.order_id AND c.status NOT IN (N'REJECTED', N'REVERSED') "
                + "WHERE o.referred_by_affiliate_id IS NOT NULL AND " + DONE + " AND o.created_at >= DATEADD(DAY, ?, GETDATE()) "
                + "GROUP BY CAST(o.created_at AS DATE) ORDER BY d";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getDate("d").toString(), rs.getInt("n"), rs.getBigDecimal("revenue"), rs.getBigDecimal("commission")});
                }
            }
        }
        return list;
    }

    public List<Object[]> topAffiliates(int days, int limit) throws SQLException {
        String sql = "SELECT TOP (?) a.name, COUNT(*) AS n, SUM(o.amount) AS revenue, ISNULL(SUM(c.amount), 0) AS commission "
                + "FROM Orders o JOIN Affiliates a ON a.affiliate_id = o.referred_by_affiliate_id "
                + "LEFT JOIN Commissions c ON c.order_id = o.order_id AND c.status NOT IN (N'REJECTED', N'REVERSED') "
                + "WHERE " + DONE + " AND o.created_at >= DATEADD(DAY, ?, GETDATE()) "
                + "GROUP BY a.name ORDER BY SUM(o.amount) DESC";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, -days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString("name"), rs.getInt("n"), rs.getBigDecimal("revenue"), rs.getBigDecimal("commission")});
                }
            }
        }
        return list;
    }

    public List<Object[]> byCampaign(int days) throws SQLException {
        String sql = "SELECT k.name, COUNT(*) AS n, SUM(o.amount) AS revenue, ISNULL(SUM(c.amount), 0) AS commission "
                + "FROM Orders o JOIN Campaigns k ON k.campaign_id = o.referral_campaign_id "
                + "LEFT JOIN Commissions c ON c.order_id = o.order_id AND c.status NOT IN (N'REJECTED', N'REVERSED') "
                + "WHERE " + DONE + " AND o.created_at >= DATEADD(DAY, ?, GETDATE()) GROUP BY k.name ORDER BY SUM(o.amount) DESC";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, -days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString("name"), rs.getInt("n"), rs.getBigDecimal("revenue"), rs.getBigDecimal("commission")});
                }
            }
        }
        return list;
    }

    public List<Object[]> topProducts(int days, int limit) throws SQLException {
        String sql = "SELECT TOP (?) i.product_name, SUM(i.quantity) AS qty, SUM(i.subtotal) AS revenue "
                + "FROM OrderItems i JOIN Orders o ON o.order_id = i.order_id "
                + "WHERE o.referred_by_affiliate_id IS NOT NULL AND " + DONE + " AND o.created_at >= DATEADD(DAY, ?, GETDATE()) "
                + "GROUP BY i.product_name ORDER BY SUM(i.subtotal) DESC";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, -days);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{rs.getString("product_name"), rs.getInt("qty"), rs.getBigDecimal("revenue")});
                }
            }
        }
        return list;
    }

    public List<Object[]> flags() throws SQLException {
        String sql = "SELECT a.name, a.referral_code, COUNT(*) AS n, MAX(c.created_at) AS last_at "
                + "FROM Commissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                + "WHERE c.created_at >= DATEADD(HOUR, -24, GETDATE()) GROUP BY a.name, a.referral_code HAVING COUNT(*) >= 5";
        List<Object[]> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{rs.getString("name"), rs.getString("referral_code"), rs.getInt("n"), "Burst: " + rs.getInt("n") + " referred orders within 24 hours."});
            }
        }
        String held = "SELECT a.name, a.referral_code, c.order_id, c.reason FROM Commissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id WHERE c.status = N'ON_HOLD'";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(held);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{rs.getString("name"), rs.getString("referral_code"), 1, "On hold: " + (rs.getString("reason") == null ? "waiting for review" : rs.getString("reason"))});
            }
        }
        return list;
    }
}
