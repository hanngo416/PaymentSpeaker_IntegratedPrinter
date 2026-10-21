package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.DBUtil;

public class SettingDAO {

    private static final String VAT_RATE = "vat_rate";

    public BigDecimal getVatRate() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT setting_value FROM Settings WHERE setting_key = ?")) {
            ps.setString(1, VAT_RATE);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new BigDecimal(rs.getString(1)) : BigDecimal.ZERO;
            }
        }
    }

    public void setVatRate(BigDecimal rate) throws SQLException {
        String sql = "UPDATE Settings SET setting_value = ? WHERE setting_key = ?; "
                + "IF @@ROWCOUNT = 0 INSERT INTO Settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rate.toPlainString());
            ps.setString(2, VAT_RATE);
            ps.setString(3, VAT_RATE);
            ps.setString(4, rate.toPlainString());
            ps.executeUpdate();
        }
    }
}
