package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.view.TicketRow;
import util.DBUtil;

public class TicketDAO {

    public int create(int deviceId, int reportedBy, String description) throws SQLException {
        String sql = "INSERT INTO MaintenanceTickets (device_id, reported_by, issue_description) VALUES (?, ?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, deviceId);
            ps.setInt(2, reportedBy);
            ps.setString(3, description);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    // status null = all. Open tickets first, then the newest; capped so the page stays light
    public List<TicketRow> search(String status) throws SQLException {
        String sql = "SELECT t.ticket_id, d.device_code, u.user_code, t.issue_description, t.status, t.tech_notes, "
                + "t.created_at, t.resolved_at FROM MaintenanceTickets t "
                + "JOIN Devices d ON d.device_id = t.device_id JOIN Users u ON u.user_id = t.reported_by "
                + (status == null ? "" : "WHERE t.status = ? ")
                + "ORDER BY CASE WHEN t.status = N'RESOLVED' THEN 1 ELSE 0 END, t.created_at DESC, t.ticket_id DESC "
                + "OFFSET 0 ROWS FETCH NEXT 200 ROWS ONLY";
        List<TicketRow> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (status != null) {
                ps.setString(1, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TicketRow r = new TicketRow();
                    r.setTicketId(rs.getInt("ticket_id"));
                    r.setDeviceCode(rs.getString("device_code"));
                    r.setReportedBy(rs.getString("user_code"));
                    r.setDescription(rs.getString("issue_description"));
                    r.setStatus(rs.getString("status"));
                    r.setTechNotes(rs.getString("tech_notes"));
                    r.setCreatedAt(rs.getTimestamp("created_at"));
                    r.setResolvedAt(rs.getTimestamp("resolved_at"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    // A resolved ticket is closed for good
    public boolean update(int ticketId, String status, String techNotes) throws SQLException {
        String sql = "UPDATE MaintenanceTickets SET status = ?, tech_notes = ?, "
                + "resolved_at = CASE WHEN ? = N'RESOLVED' THEN SYSDATETIME() ELSE NULL END "
                + "WHERE ticket_id = ? AND status <> N'RESOLVED'";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, techNotes);
            ps.setString(3, status);
            ps.setInt(4, ticketId);
            return ps.executeUpdate() == 1;
        }
    }
}
