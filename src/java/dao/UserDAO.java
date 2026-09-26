package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.User;
import model.enums.Role;
import model.enums.UserStatus;
import util.DBContext;

/** Chỉ chứa SQL cho bảng Users. Không chứa luật nghiệp vụ. */
public class UserDAO {

    private static final String SELECT_BY_USERNAME
            = "SELECT u.user_id, u.username, u.password_hash, u.salt, u.full_name, "
            + "       u.store_id, u.status, r.role_name "
            + "FROM Users u JOIN Roles r ON r.role_id = u.role_id "
            + "WHERE u.username = ?";

    /** Tìm user theo username, trả về null nếu không có. */
    public User findByUsername(String username) throws SQLException {
        try (Connection con = DBContext.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setSalt(rs.getString("salt"));
        u.setFullName(rs.getString("full_name"));
        int storeId = rs.getInt("store_id");
        u.setStoreId(rs.wasNull() ? null : storeId);   // wasNull(): phân biệt NULL với 0
        u.setStatus(UserStatus.valueOf(rs.getString("status")));
        u.setRole(Role.valueOf(rs.getString("role_name")));
        return u;
    }
}
