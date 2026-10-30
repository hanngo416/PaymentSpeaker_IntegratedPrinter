package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import util.DBUtil;

public class GuideDAO {

    public static final String BOOKMARK = "BOOKMARK";
    public static final String CHECKLIST = "CHECKLIST";
    public static final String READ = "READ";

    public static String key(String kind, String articleId) {
        return kind + ":" + articleId;
    }

    public Map<String, String> load(int userId) throws SQLException {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT article_id, kind, state FROM GuideProgress WHERE user_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    map.put(key(rs.getString("kind"), rs.getString("article_id")), rs.getString("state"));
                }
            }
        }
        return map;
    }

    public void save(int userId, String articleId, String kind, String state) throws SQLException {
        String update = "UPDATE GuideProgress SET state = ?, updated_at = SYSDATETIME() WHERE user_id = ? AND article_id = ? AND kind = ?";
        String insert = "INSERT INTO GuideProgress (user_id, article_id, kind, state) VALUES (?, ?, ?, ?)";
        try (Connection con = DBUtil.getConnection()) {
            int changed;
            try (PreparedStatement ps = con.prepareStatement(update)) {
                ps.setString(1, state);
                ps.setInt(2, userId);
                ps.setString(3, articleId);
                ps.setString(4, kind);
                changed = ps.executeUpdate();
            }
            if (changed == 0) {
                try (PreparedStatement ps = con.prepareStatement(insert)) {
                    ps.setInt(1, userId);
                    ps.setString(2, articleId);
                    ps.setString(3, kind);
                    ps.setString(4, state);
                    ps.executeUpdate();
                }
            }
        }
    }
}
