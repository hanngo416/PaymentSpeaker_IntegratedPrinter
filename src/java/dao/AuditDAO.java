package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.view.AuditRow;
import util.DBUtil;

public class AuditDAO {

    private static final String COLUMNS
            = "SELECT log_id, actor_username, action, target_entity, entity_id, detail_json, created_at "
            + "FROM AuditLogs ";

    public void append(String actor, String action, String entity, String entityId, String detail) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO AuditLogs (actor_username, action, target_entity, entity_id, detail_json, created_at) "
                     + "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, actor);
            ps.setString(2, action);
            ps.setString(3, entity);
            ps.setString(4, entityId);
            ps.setString(5, detail);
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        }
    }

    // q matches the actor, the entity id or the detail; actions (any of), from and to may be null or empty
    public Page<AuditRow> search(int page, int pageSize, String q, List<String> actions, java.sql.Date from, java.sql.Date to)
            throws SQLException {
        StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (q != null) {
            where.append("AND (actor_username LIKE ? OR entity_id LIKE ? OR detail_json LIKE ?) ");
            for (int k = 0; k < 3; k++) {
                args.add("%" + q + "%");
            }
        }
        if (actions != null && !actions.isEmpty()) {
            StringBuilder marks = new StringBuilder();
            for (String a : actions) {
                marks.append(marks.length() == 0 ? "?" : ", ?");
                args.add(a);
            }
            where.append("AND action IN (").append(marks).append(") ");
        }
        if (from != null) {
            where.append("AND created_at >= ? ");
            args.add(from);
        }
        if (to != null) {
            where.append("AND created_at < DATEADD(DAY, 1, CAST(? AS DATETIME2)) ");
            args.add(to);
        }
        int total;
        List<AuditRow> items = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM AuditLogs " + where)) {
                bind(ps, args, 0);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    COLUMNS + where + "ORDER BY log_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")) {
                bind(ps, args, 0);
                ps.setInt(args.size() + 1, (page - 1) * pageSize);
                ps.setInt(args.size() + 2, pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        items.add(map(rs));
                    }
                }
            }
        }
        return new Page<>(items, total, page, pageSize);
    }

    private static void bind(PreparedStatement ps, List<Object> args, int offset) throws SQLException {
        for (int k = 0; k < args.size(); k++) {
            ps.setObject(offset + k + 1, args.get(k));
        }
    }

    public List<String> findActions() throws SQLException {
        List<String> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT DISTINCT action FROM AuditLogs ORDER BY action");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(rs.getString(1));
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
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }
}
