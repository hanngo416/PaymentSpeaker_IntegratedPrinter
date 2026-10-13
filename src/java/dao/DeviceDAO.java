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

    private static final String SELECT_ALL
            = "SELECT device_id, device_code, device_type FROM Devices ORDER BY device_code";

    private static final String SELECT_SPEAKER_ID
            = "SELECT TOP 1 device_id FROM Devices "
            + "WHERE device_type = N'SPEAKER' ORDER BY device_id";

    private static final String PING_SPEAKER
            = "UPDATE Devices SET last_ping = SYSDATETIME(), status = N'ONLINE' "
            + "WHERE device_type = N'SPEAKER' AND status <> N'MAINTENANCE'";

    // Each time the speaker page polls, it counts as a heartbeat
    public void pingSpeaker() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(PING_SPEAKER)) {
            ps.executeUpdate();
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
                list.add(d);
            }
        }
        return list;
    }

    public Integer findSpeakerId() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_SPEAKER_ID)) {
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }
}
