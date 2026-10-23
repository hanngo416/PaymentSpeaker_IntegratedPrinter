package dao;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.view.ShiftRow;
import util.DBUtil;

public class ShiftDAO {

    public static final String OPEN = "OPEN_SHIFT";
    public static final String CLOSE = "CLOSE_SHIFT";

    private static final String COLUMNS
            = "SELECT actor_username, action, entity_id, detail_json, created_at FROM AuditLogs ";
    private static final String ONLY_SHIFTS = "WHERE action IN (N'" + OPEN + "', N'" + CLOSE + "') ";

    public ShiftRow findOpen() throws SQLException {
        String id = null;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT TOP 1 action, entity_id FROM AuditLogs " + ONLY_SHIFTS + "ORDER BY log_id DESC");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && OPEN.equals(rs.getString("action"))) {
                id = rs.getString("entity_id");
            }
        }
        return id == null ? null : find(id);
    }

    public ShiftRow find(String id) throws SQLException {
        Map<String, ShiftRow> map = new LinkedHashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     COLUMNS + ONLY_SHIFTS + "AND entity_id = ? ORDER BY log_id")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    apply(map, rs);
                }
            }
        }
        ShiftRow row = map.get(id);
        return row == null || row.getOpenedAt() == null ? null : row;
    }

    public List<ShiftRow> findRecent(int limit) throws SQLException {
        Map<String, ShiftRow> map = new LinkedHashMap<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT TOP (?) actor_username, action, entity_id, detail_json, created_at FROM AuditLogs "
                     + ONLY_SHIFTS + "ORDER BY log_id DESC")) {
            ps.setInt(1, limit * 2 + 2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    apply(map, rs);
                }
            }
        }
        List<ShiftRow> list = new ArrayList<>();
        for (ShiftRow row : map.values()) {
            if (row.getOpenedAt() != null && list.size() < limit) {
                list.add(row);
            }
        }
        return list;
    }

    private static void apply(Map<String, ShiftRow> map, ResultSet rs) throws SQLException {
        String id = rs.getString("entity_id");
        ShiftRow row = map.get(id);
        if (row == null) {
            row = new ShiftRow();
            row.setId(id);
            map.put(id, row);
        }
        if (OPEN.equals(rs.getString("action"))) {
            row.setOpenedBy(rs.getString("actor_username"));
            row.setOpenedAt(rs.getTimestamp("created_at"));
        } else {
            row.setClosedBy(rs.getString("actor_username"));
            row.setClosedAt(rs.getTimestamp("created_at"));
            readDetail(row, rs.getString("detail_json"));
        }
    }

    private static void readDetail(ShiftRow row, String json) {
        if (json == null || json.isEmpty()) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("counted_cash")) {
                row.setCountedCash(new BigDecimal(o.get("counted_cash").getAsString()));
            }
            row.setForced(o.has("forced") && "true".equals(o.get("forced").getAsString()));
        } catch (RuntimeException e) {
        }
    }
}
