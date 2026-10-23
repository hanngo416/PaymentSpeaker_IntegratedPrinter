package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.DiscountCode;
import util.DBUtil;

public class DiscountDAO {

    public static final String USABLE
            = "active = 1 AND CAST(GETDATE() AS DATE) BETWEEN valid_from AND valid_to "
            + "AND (max_uses IS NULL OR used_count < max_uses)";

    private static final String SELECT
            = "SELECT code_id, code, kind, value, holder, max_uses, used_count, valid_from, valid_to, active, "
            + "CASE WHEN " + USABLE + " THEN 1 ELSE 0 END AS available FROM DiscountCodes ";

    public DiscountCode findByCode(String code) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<DiscountCode> findSharedUsable() throws SQLException {
        return query(SELECT + "WHERE holder IS NULL AND " + USABLE + " ORDER BY code");
    }

    public List<DiscountCode> findAll() throws SQLException {
        return query(SELECT + "ORDER BY created_at DESC, code_id DESC");
    }

    public void insert(DiscountCode d) throws SQLException {
        String sql = "INSERT INTO DiscountCodes (code, kind, value, holder, max_uses, valid_from, valid_to) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, d.getCode());
            ps.setString(2, d.getKind());
            ps.setBigDecimal(3, d.getValue());
            ps.setString(4, d.getHolder());
            if (d.getMaxUses() == null) {
                ps.setNull(5, java.sql.Types.INTEGER);
            } else {
                ps.setInt(5, d.getMaxUses());
            }
            ps.setDate(6, d.getValidFrom());
            ps.setDate(7, d.getValidTo());
            ps.executeUpdate();
        }
    }

    public boolean setActive(int id, boolean active) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE DiscountCodes SET active = ? WHERE code_id = ?")) {
            ps.setBoolean(1, active);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean deleteUnused(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM DiscountCodes WHERE code_id = ? AND used_count = 0")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public static boolean consume(Connection con, String code) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE DiscountCodes SET used_count = used_count + 1 WHERE code = ? AND " + USABLE)) {
            ps.setString(1, code);
            return ps.executeUpdate() == 1;
        }
    }

    private List<DiscountCode> query(String sql) throws SQLException {
        List<DiscountCode> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private DiscountCode map(ResultSet rs) throws SQLException {
        DiscountCode d = new DiscountCode();
        d.setCodeId(rs.getInt("code_id"));
        d.setCode(rs.getString("code"));
        d.setKind(rs.getString("kind"));
        d.setValue(rs.getBigDecimal("value"));
        d.setHolder(rs.getString("holder"));
        int max = rs.getInt("max_uses");
        d.setMaxUses(rs.wasNull() ? null : max);
        d.setUsedCount(rs.getInt("used_count"));
        d.setValidFrom(rs.getDate("valid_from"));
        d.setValidTo(rs.getDate("valid_to"));
        d.setActive(rs.getBoolean("active"));
        d.setAvailable(rs.getInt("available") == 1);
        return d;
    }
}
