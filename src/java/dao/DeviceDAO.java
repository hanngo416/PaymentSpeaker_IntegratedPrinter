package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Device;
import util.DBUtil;

public class DeviceDAO {

    // A device that has not pinged for this long is shown OFFLINE
    public static final int OFFLINE_AFTER_SECONDS = 90;

    private static final String SELECT_ALL
            = "SELECT device_id, device_code, device_type, model_name, last_ping, "
            + "CASE WHEN status = N'MAINTENANCE' THEN N'MAINTENANCE' "
            + "WHEN last_ping < DATEADD(SECOND, -" + OFFLINE_AFTER_SECONDS + ", SYSDATETIME()) THEN N'OFFLINE' "
            + "ELSE N'ONLINE' END AS shown_status "
            + "FROM Devices ORDER BY device_code";


    private static final String PING
            = "UPDATE Devices SET last_ping = SYSDATETIME(), status = N'ONLINE' "
            + "WHERE device_type = ? AND status <> N'MAINTENANCE'";

    // Each time the printer page opens, it counts as a heartbeat
    public void ping(String deviceType) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(PING)) {
            ps.setString(1, deviceType);
            ps.executeUpdate();
        }
    }

    // True when a technician put every device of this type into maintenance
    public boolean isInMaintenance(String deviceType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Devices WHERE device_type = ? "
                + "AND NOT EXISTS (SELECT 1 FROM Devices d2 WHERE d2.device_type = ? AND d2.status <> N'MAINTENANCE')";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, deviceType);
            ps.setString(2, deviceType);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public Device findById(int deviceId) throws SQLException {
        for (Device d : findAll()) {
            if (d.getDeviceId() == deviceId) {
                return d;
            }
        }
        return null;
    }

    // Maintenance is set by hand; ONLINE puts the device back to work (the next ping keeps it fresh)
    public boolean setMaintenance(int deviceId, boolean maintenance) throws SQLException {
        String sql = "UPDATE Devices SET status = ?, last_ping = SYSDATETIME() WHERE device_id = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maintenance ? "MAINTENANCE" : "ONLINE");
            ps.setInt(2, deviceId);
            return ps.executeUpdate() == 1;
        }
    }

    public List<Device> findAll() throws SQLException {
        List<Device> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Device d = new Device();
                d.setDeviceId(rs.getInt("device_id"));
                d.setDeviceCode(rs.getString("device_code"));
                d.setDeviceType(rs.getString("device_type"));
                d.setModelName(rs.getString("model_name"));
                d.setStatus(rs.getString("shown_status"));
                d.setLastPing(rs.getTimestamp("last_ping"));
                list.add(d);
            }
        }
        return list;
    }
}
