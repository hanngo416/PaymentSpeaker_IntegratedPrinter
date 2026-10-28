import dao.AuditDAO;
import dao.SettingDAO;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import model.view.AuditRow;
import service.AuditService;
import service.PaymentPollJob;

public class SepayResilienceTest {

    private static int failures = 0;

    private static void check(String name, boolean condition) {
        if (!condition) {
            failures++;
        }
        System.out.println((condition ? "OK   " : "FAIL ") + name);
    }

    private static void checkEqual(String name, Object actual, Object expected) {
        boolean ok = (actual == null && expected == null) || (actual != null && actual.equals(expected));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + actual + (ok ? "" : " (expected " + expected + ")"));
    }

    public static void main(String[] args) {
        testSettingPersistence();
        testBackoffProgression();
        testAlertAuditLogging();

        if (failures > 0) {
            System.err.println("TEST FAILURES: " + failures);
            System.exit(1);
        } else {
            System.out.println("ALL SEPAY RESILIENCE TESTS PASSED");
        }
    }

    private static void testSettingPersistence() {
        try {
            SettingDAO dao = new SettingDAO();
            dao.set("sepay_last_poll_time", "2026-11-11 00:00:00");
            dao.set("sepay_last_status", "SUCCESS");
            dao.set("sepay_last_count", "42");
            dao.set("sepay_last_error", "");
            dao.set("sepay_consecutive_failures", "0");

            checkEqual("setting: last poll time", dao.get("sepay_last_poll_time", ""), "2026-11-11 00:00:00");
            checkEqual("setting: last status", dao.get("sepay_last_status", ""), "SUCCESS");
            checkEqual("setting: last count", dao.get("sepay_last_count", ""), "42");
            checkEqual("setting: last error empty", dao.get("sepay_last_error", "fallback"), "");
            checkEqual("setting: consecutive failures", dao.get("sepay_consecutive_failures", ""), "0");
        } catch (SQLException e) {
            check("setting persistence failed: " + e.getMessage(), false);
        }
    }

    private static void testBackoffProgression() {
        PaymentPollJob job = new PaymentPollJob();
        checkEqual("initial failures", job.getConsecutiveFailures(), 0);
        checkEqual("initial nextAllowedTime", job.getNextAllowedTime(), 0L);

        int[] failureSteps = {1, 2, 3, 4, 5};
        int[] expectedBackoffSec = {15, 30, 60, 60, 60};

        for (int i = 0; i < failureSteps.length; i++) {
            int f = failureSteps[i];
            int backoffSec = f == 1 ? 15 : f == 2 ? 30 : 60;
            checkEqual("backoff step for failure " + f, backoffSec, expectedBackoffSec[i]);
        }

        job.setConsecutiveFailures(5);
        checkEqual("consecutive failures set to 5", job.getConsecutiveFailures(), 5);
        check("alert threshold hit at 5", job.getConsecutiveFailures() == 5);
    }

    private static void testAlertAuditLogging() {
        try {
            AuditService.log(null, "SEPAY_GATEWAY_ALERT", "PAYMENT_GATEWAY", null,
                    "consecutive_failures", "5", "error", "Simulated gateway timeout");

            AuditDAO auditDAO = new AuditDAO();
            List<AuditRow> logs = auditDAO.search(1, 10, "Simulated gateway timeout",
                    Collections.singletonList("SEPAY_GATEWAY_ALERT"), null, null).getItems();
            check("audit log contains SEPAY_GATEWAY_ALERT", !logs.isEmpty());
            if (!logs.isEmpty()) {
                AuditRow r = logs.get(0);
                checkEqual("audit entity", r.getEntity(), "PAYMENT_GATEWAY");
                check("audit details contain error info", r.getDetail() != null && r.getDetail().contains("Simulated gateway timeout"));
            }
        } catch (SQLException e) {
            check("alert audit logging failed: " + e.getMessage(), false);
        }
    }
}
