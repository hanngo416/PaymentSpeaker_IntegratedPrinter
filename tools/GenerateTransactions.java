import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Random;

/*
 * Fills the Transactions table with generated rows so the transaction list,
 * paging and chart can be tested with a large table.
 *
 * Compile and run from the project folder:
 *     javac -cp web/WEB-INF/lib/sqljdbc4.jar -d build/tools src/java/util/DBUtil.java tools/GenerateTransactions.java
 *     java  -cp "build/tools;web/WEB-INF/lib/sqljdbc4.jar" GenerateTransactions 20000
 *     java  -cp "build/tools;web/WEB-INF/lib/sqljdbc4.jar" GenerateTransactions clear
 *
 * Generated rows have a bank_trans_id starting with GEN, are already marked as
 * announced so the speaker stays quiet, and never create alerts or orders.
 */
public class GenerateTransactions {

    private static final int DAYS = 90;
    private static final int BATCH = 1000;

    public static void main(String[] args) throws Exception {
        String arg = args.length > 0 ? args[0] : "20000";
        try (Connection cn = util.DBUtil.getConnection()) {
            cn.setAutoCommit(false);
            try (PreparedStatement del = cn.prepareStatement(
                    "DELETE FROM Transactions WHERE bank_trans_id LIKE 'GEN%'")) {
                System.out.println("Removed " + del.executeUpdate() + " generated rows");
            }
            if (arg.equalsIgnoreCase("clear")) {
                cn.commit();
                return;
            }
            int count = Integer.parseInt(arg);
            int deviceId = speakerId(cn);
            String sql = "INSERT INTO Transactions (bank_trans_id, device_id, amount, content, "
                    + "bank_account, gateway, status, trans_time, received_at, announced, printed) "
                    + "VALUES (?, ?, ?, ?, ?, N'SEPAY', ?, ?, ?, 1, 0)";
            Random rnd = new Random(42);
            long now = System.currentTimeMillis();
            long span = DAYS * 24L * 3600 * 1000;
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                for (int i = 1; i <= count; i++) {
                    Timestamp when = new Timestamp(now - (long) (rnd.nextDouble() * span));
                    int amount = (10 + rnd.nextInt(490)) * 1000;
                    int roll = rnd.nextInt(100);
                    String status = roll < 94 ? "LEGIT" : roll < 98 ? "NO_ORDER" : "DUPLICATE";
                    ps.setString(1, String.format("GEN%08d", i));
                    ps.setInt(2, deviceId);
                    ps.setBigDecimal(3, new java.math.BigDecimal(amount));
                    ps.setString(4, "Generated transfer " + i);
                    ps.setString(5, "0987654321");
                    ps.setString(6, status);
                    ps.setTimestamp(7, when);
                    ps.setTimestamp(8, when);
                    ps.addBatch();
                    if (i % BATCH == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }
            cn.commit();
            System.out.println("Inserted " + count + " rows");
        }
    }

    private static int speakerId(Connection cn) throws Exception {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT TOP 1 device_id FROM Devices WHERE device_type = N'SPEAKER' ORDER BY device_id");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("No speaker device found, run 02_seed.sql first");
            }
            return rs.getInt(1);
        }
    }
}
