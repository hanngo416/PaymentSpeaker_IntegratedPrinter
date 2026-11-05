package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import util.DBUtil;

public class SettingDAO {

    private static final String VAT_RATE = "vat_rate";

    public String get(String key, String fallback) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT TOP 1 setting_value FROM Settings WHERE setting_key = ? ORDER BY store_id DESC")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : fallback;
            }
        }
    }

    public void set(String key, String value) throws SQLException {
        String sql = "UPDATE Settings SET setting_value = ? WHERE setting_key = ? "
                + "AND store_id = ISNULL(NULLIF(TRY_CAST(SESSION_CONTEXT(N'store_id') AS INT), -1), 0); "
                + "IF @@ROWCOUNT = 0 INSERT INTO Settings (setting_key, setting_value) VALUES (?, ?)";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setString(2, key);
            ps.setString(3, key);
            ps.setString(4, value);
            ps.executeUpdate();
        }
    }

    public boolean getVatIncluded() throws SQLException {
        return "1".equals(get("vat_included", "1"));
    }

    public void setVatIncluded(boolean included) throws SQLException {
        set("vat_included", included ? "1" : "0");
    }

    public BigDecimal getVatRate() throws SQLException {
        return new BigDecimal(get(VAT_RATE, "0"));
    }

    public void setVatRate(BigDecimal rate) throws SQLException {
        set(VAT_RATE, rate.toPlainString());
    }
}
