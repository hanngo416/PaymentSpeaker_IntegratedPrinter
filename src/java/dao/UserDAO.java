/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.UserFilter;
import model.User;
import model.enums.Role;
import model.enums.UserStatus;
import util.DBUtil;

/**
 * Holds only the SQL for the Users table. No business rules here.
 *
 * @author PC
 */
public class UserDAO {

    private static final String SELECT_BY_ID
            = "SELECT u.user_id, u.user_code, u.full_name, "
            + "       u.status, u.email, u.google_id, r.role_name "
            + "FROM Users u JOIN Roles r ON r.role_id = u.role_id "
            + "WHERE u.user_id = ?";

    private static final String SELECT_BY_EMAIL
            = "SELECT u.user_id, u.user_code, u.full_name, "
            + "       u.status, u.email, u.google_id, r.role_name "
            + "FROM Users u JOIN Roles r ON r.role_id = u.role_id "
            + "WHERE u.email = ?";

    private static final String UPDATE_GOOGLE_ID
            = "UPDATE Users SET google_id = ? WHERE user_id = ?";

    public User findById(int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_ID)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public void updateGoogleId(int userId, String googleId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_GOOGLE_ID)) {
            ps.setString(1, googleId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    private static final String INSERT_USER
            = "INSERT INTO Users (user_code, full_name, email, role_id, status) "
            + "VALUES (?, ?, ?, (SELECT role_id FROM Roles WHERE role_name = ?), N'ACTIVE')";

    private static final String UPDATE_USER
            = "UPDATE Users SET full_name = ?, email = ?, google_id = ?, "
            + "role_id = (SELECT role_id FROM Roles WHERE role_name = ?) "
            + "WHERE user_id = ?";

    private static final String UPDATE_STATUS
            = "UPDATE Users SET status = ? WHERE user_id = ?";

    private static final String CLEAR_GOOGLE_ID
            = "UPDATE Users SET google_id = NULL WHERE user_id = ?";

    private static final String COUNT_ACTIVE_ADMINS
            = "SELECT COUNT(*) FROM Users u JOIN Roles r ON r.role_id = u.role_id "
            + "WHERE r.role_name = N'ADMIN' AND u.status = N'ACTIVE'";

    private static final String COUNT_EMAIL
            = "SELECT COUNT(*) FROM Users WHERE email = ? AND user_id <> ?";

    private static final String COUNT_USER_CODE
            = "SELECT COUNT(*) FROM Users WHERE user_code = ? AND user_id <> ?";

    private static final String LIST_COLUMNS
            = "SELECT u.user_id, u.user_code, u.full_name, "
            + "       u.status, u.email, u.google_id, r.role_name";

    public Page<User> search(UserFilter f) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (f.getKeyword() != null && !f.getKeyword().isEmpty()) {
            where.append(" AND (u.user_code LIKE ? OR u.full_name LIKE ? OR u.email LIKE ?)");
            String like = "%" + f.getKeyword() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (f.getRole() != null) {
            where.append(" AND r.role_name = ?");
            params.add(f.getRole());
        }
        if (f.getStatus() != null) {
            where.append(" AND u.status = ?");
            params.add(f.getStatus());
        }
        String from = " FROM Users u JOIN Roles r ON r.role_id = u.role_id" + where;
        String order = " ORDER BY " + sortColumn(f.getSort())
                + ("desc".equals(f.getDir()) ? " DESC" : " ASC")
                + ", u.user_id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        int total;
        List<User> items = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*)" + from)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(LIST_COLUMNS + from + order)) {
                bind(ps, params);
                ps.setInt(params.size() + 1, f.getOffset());
                ps.setInt(params.size() + 2, f.getPageSize());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
        }
        return new Page<>(items, total, f.getPage(), f.getPageSize());
    }

    public void insert(User u) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_USER)) {
            ps.setString(1, u.getUserCode());
            ps.setString(2, u.getFullName());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRole().name());
            ps.executeUpdate();
        }
    }

    public void update(User u) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_USER)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getGoogleId());
            ps.setString(4, u.getRole().name());
            ps.setInt(5, u.getUserId());
            ps.executeUpdate();
        }
    }

    public void setStatus(int userId, UserStatus status) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(UPDATE_STATUS)) {
            ps.setString(1, status.name());
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void clearGoogleId(int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(CLEAR_GOOGLE_ID)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public boolean delete(int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement otps = con.prepareStatement("DELETE FROM LoginOtps WHERE user_id = ?");
                 PreparedStatement user = con.prepareStatement("DELETE FROM Users WHERE user_id = ?")) {
                otps.setInt(1, userId);
                otps.executeUpdate();
                user.setInt(1, userId);
                int deleted = user.executeUpdate();
                con.commit();
                return deleted == 1;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public int countActiveAdmins() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_ACTIVE_ADMINS);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public boolean emailExists(String email, int exceptUserId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_EMAIL)) {
            ps.setString(1, email);
            ps.setInt(2, exceptUserId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public boolean userCodeExists(String code, int exceptUserId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_USER_CODE)) {
            ps.setString(1, code);
            ps.setInt(2, exceptUserId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public String suggestNextUserCode(String baseCode) throws SQLException {
        if (baseCode == null || baseCode.trim().isEmpty()) {
            return "user1";
        }
        String trimmed = baseCode.trim();
        String root = trimmed.replaceAll("\\d+$", "");
        if (root.isEmpty()) {
            root = trimmed;
        }
        int num = 2;
        while (userCodeExists(root + num, 0)) {
            num++;
        }
        return root + num;
    }

    private static String sortColumn(String sort) {
        switch (sort) {
            case "name":
                return "u.full_name";
            case "role":
                return "r.role_name";
            case "status":
                return "u.status";
            default:
                return "u.user_code";
        }
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUserCode(rs.getString("user_code"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setGoogleId(rs.getString("google_id"));
        u.setStatus(UserStatus.valueOf(rs.getString("status")));
        u.setRole(Role.valueOf(rs.getString("role_name")));
        return u;
    }
}
