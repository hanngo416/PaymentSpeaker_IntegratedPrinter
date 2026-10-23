package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Affiliate;
import util.DBUtil;

public class AffiliateDAO {

    private static final String COLUMNS
            = "a.affiliate_id, a.name, a.rate, a.active, a.referral_code, a.phone, a.email, "
            + "a.bank_name, a.bank_account, a.bank_holder, a.status, a.created_at";

    private static final String SELECT = "SELECT " + COLUMNS + " FROM Affiliates a ";

    private static final String SELECT_STATS = "SELECT " + COLUMNS + ", "
            + "(SELECT COUNT(*) FROM Orders o WHERE o.referred_by_affiliate_id = a.affiliate_id AND o.status IN (N'COMPLETED', N'PAID')) AS order_count, "
            + "ISNULL((SELECT SUM(o.amount) FROM Orders o WHERE o.referred_by_affiliate_id = a.affiliate_id AND o.status IN (N'COMPLETED', N'PAID')), 0) AS revenue, "
            + "ISNULL((SELECT SUM(c.amount) FROM Commissions c WHERE c.affiliate_id = a.affiliate_id AND c.status NOT IN (N'REJECTED', N'REVERSED')), 0) AS commission_total "
            + "FROM Affiliates a ";

    public List<Affiliate> findAll() throws SQLException {
        List<Affiliate> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "ORDER BY a.name, a.affiliate_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs, false));
            }
        }
        return list;
    }

    public List<Affiliate> search(String q, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_STATS).append("WHERE 1 = 1 ");
        List<String> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND (a.name LIKE ? OR a.referral_code LIKE ? OR a.email LIKE ? OR a.phone LIKE ?) ");
            String like = "%" + q + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (status != null && !status.isEmpty()) {
            sql.append("AND a.status = ? ");
            args.add(status);
        }
        sql.append("ORDER BY a.created_at DESC, a.affiliate_id DESC");
        List<Affiliate> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < args.size(); i++) {
                ps.setString(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs, true));
                }
            }
        }
        return list;
    }

    public Affiliate find(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_STATS + "WHERE a.affiliate_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, true) : null;
            }
        }
    }

    public Affiliate findByCode(String code) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE a.referral_code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, false) : null;
            }
        }
    }

    public Affiliate findForEmail(String email) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE a.email = ?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs, false) : null;
            }
        }
    }

    public boolean updateBank(int id, String bankName, String bankAccount, String bankHolder) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE Affiliates SET bank_name = ?, bank_account = ?, bank_holder = ? WHERE affiliate_id = ?")) {
            ps.setString(1, bankName);
            ps.setString(2, bankAccount);
            ps.setString(3, bankHolder);
            ps.setInt(4, id);
            return ps.executeUpdate() == 1;
        }
    }

    public void insert(Affiliate a) throws SQLException {
        String sql = "INSERT INTO Affiliates (name, rate, referral_code, phone, email, bank_name, bank_account, bank_holder, status, active) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getName());
            ps.setBigDecimal(2, a.getRate());
            ps.setString(3, a.getReferralCode());
            ps.setString(4, blank(a.getPhone()));
            ps.setString(5, blank(a.getEmail()));
            ps.setString(6, blank(a.getBankName()));
            ps.setString(7, blank(a.getBankAccount()));
            ps.setString(8, blank(a.getBankHolder()));
            ps.setString(9, a.getStatus());
            ps.setBoolean(10, Affiliate.ACTIVE.equals(a.getStatus()));
            ps.executeUpdate();
        }
    }

    public boolean updateProfile(Affiliate a) throws SQLException {
        String sql = "UPDATE Affiliates SET name = ?, rate = ?, phone = ?, email = ?, bank_name = ?, bank_account = ?, bank_holder = ? WHERE affiliate_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getName());
            ps.setBigDecimal(2, a.getRate());
            ps.setString(3, blank(a.getPhone()));
            ps.setString(4, blank(a.getEmail()));
            ps.setString(5, blank(a.getBankName()));
            ps.setString(6, blank(a.getBankAccount()));
            ps.setString(7, blank(a.getBankHolder()));
            ps.setInt(8, a.getAffiliateId());
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setStatus(int id, String status) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE Affiliates SET status = ?, active = ? WHERE affiliate_id = ?")) {
            ps.setString(1, status);
            ps.setBoolean(2, Affiliate.ACTIVE.equals(status));
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteUnused(int id) throws SQLException {
        String sql = "DELETE FROM Affiliates WHERE affiliate_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM ChannelSales WHERE affiliate_id = ?) "
                + "AND NOT EXISTS (SELECT 1 FROM Orders WHERE referred_by_affiliate_id = ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ps.setInt(3, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static String blank(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private Affiliate map(ResultSet rs, boolean stats) throws SQLException {
        Affiliate a = new Affiliate();
        a.setAffiliateId(rs.getInt("affiliate_id"));
        a.setName(rs.getString("name"));
        a.setRate(rs.getBigDecimal("rate"));
        a.setActive(rs.getBoolean("active"));
        a.setReferralCode(rs.getString("referral_code"));
        a.setPhone(rs.getString("phone"));
        a.setEmail(rs.getString("email"));
        a.setBankName(rs.getString("bank_name"));
        a.setBankAccount(rs.getString("bank_account"));
        a.setBankHolder(rs.getString("bank_holder"));
        a.setStatus(rs.getString("status"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        if (stats) {
            a.setOrderCount(rs.getInt("order_count"));
            a.setRevenue(rs.getBigDecimal("revenue"));
            a.setCommissionTotal(rs.getBigDecimal("commission_total"));
        }
        return a;
    }
}
