import dao.AuditDAO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import model.User;
import service.AuditService;
import util.DBUtil;

public class AuditChainTest {

    private static int failures = 0;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static String one(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void run(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    private static int count(String q, Object... args) throws Exception {
        return Integer.parseInt(one(q, args));
    }

    private static String sha256(String text) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_16LE));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02X", b));
        }
        return hex.toString();
    }

    private static String failureOf(String sql, Object... args) {
        try {
            run(sql, args);
            return "allowed";
        } catch (Exception e) {
            return e.getMessage() != null && e.getMessage().contains("cannot be changed or deleted") ? "blocked" : "other: " + e.getMessage();
        }
    }

    public static void main(String[] args) throws Exception {
        AuditDAO dao = new AuditDAO();
        String tag = "TXAUDIT" + (System.currentTimeMillis() % 100000);
        AuditDAO.Verification before = dao.verify();
        check("chain is intact before the test", before.isIntact(), true);
        int rowsBefore = count("SELECT COUNT(*) FROM AuditLogs");
        check("verify counts every row", before.getRows(), rowsBefore);

        dao.append(tag, "TEST_ONE", "TEST", "e1", "{\"k\":\"v1\"}");
        dao.append(tag, "TEST_TWO", "TEST", "e2", "{\"k\":\"v2\"}");
        dao.append(tag, "TEST_THREE", "TEST", "e3", null);
        check("three appends add three rows", count("SELECT COUNT(*) FROM AuditLogs"), rowsBefore + 3);
        check("chain is intact after the appends", dao.verify().isIntact(), true);

        int id1 = count("SELECT log_id FROM AuditLogs WHERE actor_username = ? AND action = N'TEST_ONE'", tag);
        int id2 = count("SELECT log_id FROM AuditLogs WHERE actor_username = ? AND action = N'TEST_TWO'", tag);
        int id3 = count("SELECT log_id FROM AuditLogs WHERE actor_username = ? AND action = N'TEST_THREE'", tag);
        check("log ids grow", id1 < id2 && id2 < id3, true);
        check("each row links to the hash of the row before it", one("SELECT prev_hash FROM AuditLogs WHERE log_id = ?", id2), one("SELECT curr_hash FROM AuditLogs WHERE log_id = ?", id1));
        check("the next row links to the second", one("SELECT prev_hash FROM AuditLogs WHERE log_id = ?", id3), one("SELECT curr_hash FROM AuditLogs WHERE log_id = ?", id2));
        check("hash is 64 hex characters", one("SELECT curr_hash FROM AuditLogs WHERE log_id = ?", id2).matches("[0-9A-F]{64}"), true);
        check("the chain head points to the newest row", one("SELECT last_hash FROM AuditChainHead WHERE head_id = 1"), one("SELECT curr_hash FROM AuditLogs WHERE log_id = ?", id3));
        check("a missing detail is stored as an empty string or null", one("SELECT ISNULL(detail_json, N'') FROM AuditLogs WHERE log_id = ?", id3), "");

        String prev = one("SELECT prev_hash FROM AuditLogs WHERE log_id = ?", id2);
        String created = one("SELECT CONVERT(VARCHAR(27), created_at, 121) FROM AuditLogs WHERE log_id = ?", id2);
        String expected = sha256(prev + "|" + tag + "|TEST_TWO|TEST|e2|{\"k\":\"v2\"}|" + created);
        check("the hash matches an independent SHA-256 computation", one("SELECT curr_hash FROM AuditLogs WHERE log_id = ?", id2), expected);

        check("an UPDATE of a chained row is blocked", failureOf("UPDATE AuditLogs SET detail_json = N'changed' WHERE log_id = ?", id2), "blocked");
        check("a DELETE of a chained row is blocked", failureOf("DELETE FROM AuditLogs WHERE log_id = ?", id2), "blocked");
        check("a DELETE of the newest row is blocked", failureOf("DELETE FROM AuditLogs WHERE log_id = ?", id3), "blocked");
        check("the blocked statements changed nothing", one("SELECT detail_json FROM AuditLogs WHERE log_id = ?", id2), "{\"k\":\"v2\"}");
        check("chain is still intact", dao.verify().isIntact(), true);

        User actor = new User();
        actor.setUserCode(tag);
        AuditService.log(actor, "TEST_SERVICE", "TEST", 42, "amount", "1000", "note", "x");
        check("AuditService writes the actor code", count("SELECT COUNT(*) FROM AuditLogs WHERE actor_username = ? AND action = N'TEST_SERVICE' AND entity_id = N'42'", tag), 1);
        check("AuditService writes the details as JSON", one("SELECT detail_json FROM AuditLogs WHERE actor_username = ? AND action = N'TEST_SERVICE'", tag), "{\"amount\":\"1000\",\"note\":\"x\"}");
        AuditService.log(null, "TEST_SYSTEM", "TEST", tag);
        check("a missing actor is written as SYSTEM", count("SELECT COUNT(*) FROM AuditLogs WHERE actor_username = N'SYSTEM' AND action = N'TEST_SYSTEM' AND entity_id = ?", tag), 1);

        int rowsBeforeRace = count("SELECT COUNT(*) FROM AuditLogs");
        final int threads = 4;
        final int each = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            final CyclicBarrier barrier = new CyclicBarrier(threads);
            List<Future<Integer>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int thread = t;
                futures.add(pool.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        barrier.await();
                        int failed = 0;
                        for (int i = 0; i < each; i++) {
                            try {
                                new AuditDAO().append(tag, "TEST_RACE", "TEST", thread + "-" + i, "{}");
                            } catch (SQLException e) {
                                failed++;
                            }
                        }
                        return failed;
                    }
                }));
            }
            int failed = 0;
            for (Future<Integer> f : futures) {
                failed += f.get();
            }
            check("concurrent appends: no failed append", failed, 0);
        } finally {
            pool.shutdownNow();
        }
        check("concurrent appends: every row was written", count("SELECT COUNT(*) FROM AuditLogs"), rowsBeforeRace + threads * each);
        check("concurrent appends: the chain is still intact", dao.verify().isIntact(), true);
        check("concurrent appends: no two rows share a previous hash", count("SELECT COUNT(*) FROM (SELECT prev_hash FROM AuditLogs GROUP BY prev_hash HAVING COUNT(*) > 1) x"), 0);

        String original = one("SELECT detail_json FROM AuditLogs WHERE log_id = ?", id2);
        try {
            run("ALTER TABLE AuditLogs DISABLE TRIGGER TR_AuditLogs_Immutable");
            run("UPDATE AuditLogs SET detail_json = N'tampered' WHERE log_id = ?", id2);
            AuditDAO.Verification broken = dao.verify();
            check("tampering with a record is detected", broken.isIntact(), false);
            check("the first broken record is reported", broken.getBrokenAt(), id2);
        } finally {
            run("UPDATE AuditLogs SET detail_json = ? WHERE log_id = ?", original, id2);
            run("ALTER TABLE AuditLogs ENABLE TRIGGER TR_AuditLogs_Immutable");
        }
        check("the trigger is enabled again", failureOf("UPDATE AuditLogs SET detail_json = N'x' WHERE log_id = ?", id2), "blocked");
        check("the chain is intact after the repair", dao.verify().isIntact(), true);

        String head = one("SELECT last_hash FROM AuditChainHead WHERE head_id = 1");
        try {
            run("UPDATE AuditChainHead SET last_hash = ? WHERE head_id = 1", one("SELECT prev_hash FROM AuditLogs WHERE log_id = ?", id3));
            AuditDAO.Verification cut = dao.verify();
            check("removal of the newest records is detected", cut.isIntact(), false);
            check("the removal reason is reported", cut.getReason(), "The newest records were removed.");
        } finally {
            run("UPDATE AuditChainHead SET last_hash = ? WHERE head_id = 1", head);
        }
        check("the chain is intact after the head is restored", dao.verify().isIntact(), true);

        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
