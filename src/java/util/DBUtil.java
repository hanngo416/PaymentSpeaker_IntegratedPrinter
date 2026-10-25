/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class DBUtil {
   private static final String URL =
        "jdbc:sqlserver://localhost:1433;databaseName=PaymentSpeaker";
    private static final String USER = "sa";
    private static final String PASS = "12345";

    static {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("sqljdbc4.jar is missing from web/WEB-INF/lib", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void close(ResultSet rs, Statement st, Connection cn) {
        try { if (rs != null) rs.close(); } catch (SQLException ignored) { }
        try { if (st != null) st.close(); } catch (SQLException ignored) { }
        try { if (cn != null) cn.close(); } catch (SQLException ignored) { }
    }

    public static void close(Statement st, Connection cn) {
        close(null, st, cn);
    }  

    public static BigDecimal optionalDecimal(ResultSet rs, String column) {
        try {
            return rs.getBigDecimal(column);
        } catch (SQLException e) {
            return null;
        }
    }
}
