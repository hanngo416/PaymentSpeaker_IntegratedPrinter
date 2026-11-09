package dao;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.view.AuditRow;
import util.DBUtil;
import util.StoreContext;

public class AuditDAO {

    private static final String COLUMNS
            = "SELECT log_id, actor_username, action, target_entity, entity_id, detail_json, created_at "
            + "FROM AuditLogs ";

    public void append(String actor, String action, String entity, String entityId, String detail) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             CallableStatement cs = con.prepareCall("{call usp_AuditAppend(?, ?, ?, ?, ?)}")) {
            cs.setString(1, actor);
            cs.setString(2, action);
            cs.setString(3, entity);
            cs.setString(4, entityId);
            cs.setString(5, detail);
            cs.execute();
        }
    }

    public static class Verification {

        private final boolean intact;
        private final int rows;
        private final Integer brokenAt;
        private final String reason;

        Verification(boolean intact, int rows, Integer brokenAt, String reason) {
            this.intact = intact;
            this.rows = rows;
            this.brokenAt = brokenAt;
            this.reason = reason;
        }

        public boolean isIntact() {
            return intact;
        }

        public int getRows() {
            return rows;
        }

        public Integer getBrokenAt() {
            return brokenAt;
        }

        public String getReason() {
            return reason;
        }
    }

    private static final String FIRST_BAD_ROW
            = "SELECT TOP 1 log_id FROM ("
            + "SELECT log_id, prev_hash, curr_hash, LAG(curr_hash) OVER (ORDER BY log_id) AS lag_hash, "
            + "CONVERT(CHAR(64), HASHBYTES('SHA2_256', CONCAT(prev_hash, N'|', actor_username, N'|', action, N'|', "
            + "target_entity, N'|', entity_id, N'|', ISNULL(detail_json, N''), N'|', "
            + "CONVERT(VARCHAR(27), created_at, 121))), 2) AS expected_hash FROM AuditLogs) x "
            + "WHERE curr_hash IS NULL OR prev_hash IS NULL OR curr_hash <> expected_hash "
            + "OR prev_hash <> ISNULL(lag_hash, REPLICATE('0', 64)) ORDER BY log_id";

    public Verification verify() throws SQLException {
        int[] saved = StoreContext.snapshot();
        StoreContext.set(null, true);
        try (Connection con = DBUtil.getConnection()) {
            int rows;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM AuditLogs");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                rows = rs.getInt(1);
            }
            try (PreparedStatement ps = con.prepareStatement(FIRST_BAD_ROW);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Verification(false, rows, rs.getInt(1), "This record or the link to the one before it was changed.");
                }
            }
            String last = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT TOP 1 curr_hash FROM AuditLogs ORDER BY log_id DESC");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    last = rs.getString(1);
                }
            }
            String head = null;
            try (PreparedStatement ps = con.prepareStatement("SELECT last_hash FROM AuditChainHead WHERE head_id = 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    head = rs.getString(1);
                }
            }
            boolean empty = last == null;
            boolean headOk = empty ? (head == null || head.matches("0+")) : last.equals(head);
            if (!headOk) {
                return new Verification(false, rows, null, "The newest records were removed.");
            }
            return new Verification(true, rows, null, null);
        } finally {
            StoreContext.restore(saved);
        }
    }

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
