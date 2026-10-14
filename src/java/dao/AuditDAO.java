package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.view.AuditRow;
import util.AuditHash;
import util.DBUtil;

public class AuditDAO {

    private static final String COLUMNS
            = "SELECT log_id, actor_username, action, target_entity, entity_id, detail_json, prev_hash, curr_hash, created_at "
            + "FROM AuditLogs ";

    // Adds one record to the end of the chain. The lock on the last row makes two writers queue up,
    // so every record links to exactly one previous record.
    public void append(String actor, String action, String entity, String entityId, String detail) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                String prev = AuditHash.GENESIS_PREV;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT TOP 1 curr_hash FROM AuditLogs WITH (UPDLOCK, HOLDLOCK) ORDER BY log_id DESC");
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        prev = rs.getString(1);
                    }
                }
                Timestamp now = new Timestamp(System.currentTimeMillis());
                int id;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, prev_hash, curr_hash, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, actor);
                    ps.setString(2, action);
                    ps.setString(3, entity);
                    ps.setString(4, entityId);
                    ps.setString(5, detail);
                    ps.setString(6, prev);
                    ps.setString(7, AuditHash.GENESIS_PREV);
                    ps.setTimestamp(8, now);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        id = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE AuditLogs SET curr_hash = ? WHERE log_id = ?")) {
                    ps.setString(1, AuditHash.compute(prev, id, actor, action, entity, entityId, detail, now));
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public Page<AuditRow> search(int page, int pageSize) throws SQLException {
        int total;
        List<AuditRow> items = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM AuditLogs");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                total = rs.getInt(1);
            }
            try (PreparedStatement ps = con.prepareStatement(
                    COLUMNS + "ORDER BY log_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")) {
                ps.setInt(1, (page - 1) * pageSize);
                ps.setInt(2, pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
        }
        return new Page<>(items, total, page, pageSize);
    }

    // The whole chain, oldest first, for the integrity check
    public List<AuditRow> findAll() throws SQLException {
        List<AuditRow> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COLUMNS + "ORDER BY log_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private static AuditRow map(ResultSet rs) throws SQLException {
        AuditRow r = new AuditRow();
        r.setLogId(rs.getInt("log_id"));
        r.setActor(rs.getString("actor_username"));
        r.setAction(rs.getString("action"));
        r.setEntity(rs.getString("target_entity"));
        r.setEntityId(rs.getString("entity_id"));
        r.setDetail(rs.getString("detail_json"));
        r.setPrevHash(rs.getString("prev_hash"));
        r.setCurrHash(rs.getString("curr_hash"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }
}
