package service;

import com.google.gson.JsonObject;
import dao.AuditDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.User;
import model.view.AuditRow;
import util.AuditHash;

/**
 * Writes the tamper-evident audit trail and checks it. A failure to write never breaks the action being logged.
 */
public class AuditService {

    public static final String SYSTEM = "SYSTEM";

    private static final Logger LOG = Logger.getLogger(AuditService.class.getName());
    private static final AuditDAO DAO = new AuditDAO();

    public static final class Check {

        private final boolean ok;
        private final int checked;
        private final int brokenLogId;
        private final String reason;

        Check(boolean ok, int checked, int brokenLogId, String reason) {
            this.ok = ok;
            this.checked = checked;
            this.brokenLogId = brokenLogId;
            this.reason = reason;
        }

        public boolean isOk() { return ok; }
        public int getChecked() { return checked; }
        public int getBrokenLogId() { return brokenLogId; }
        public String getReason() { return reason; }
    }

    // actor null = the system itself (for example the polling job). details are name, value, name, value...
    public static void log(User actor, String action, String entity, Object entityId, String... details) {
        JsonObject json = new JsonObject();
        for (int i = 0; i + 1 < details.length; i += 2) {
            json.addProperty(details[i], details[i + 1]);
        }
        try {
            DAO.append(actor == null ? SYSTEM : actor.getUserCode(), action, entity, String.valueOf(entityId), json.toString());
        } catch (SQLException | RuntimeException e) {
            LOG.log(Level.WARNING, "AUDIT_WRITE_FAILED action=" + action + " " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    // Walks the chain from the first record: every record must point at the hash before it and match its own hash.
    // The first record (genesis) is trusted; everything after it is proven by it.
    public Check verify() throws SQLException {
        List<AuditRow> rows = DAO.findAll();
        String previous = null;
        int checked = 0;
        for (AuditRow r : rows) {
            if (previous != null) {
                if (!previous.equals(r.getPrevHash())) {
                    return new Check(false, checked, r.getLogId(), "it does not follow the record before it (a record was removed or changed)");
                }
                String expected = AuditHash.compute(r.getPrevHash(), r.getLogId(), r.getActor(), r.getAction(),
                        r.getEntity(), r.getEntityId(), r.getDetail(), r.getCreatedAt());
                if (!expected.equals(r.getCurrHash())) {
                    return new Check(false, checked, r.getLogId(), "its content no longer matches its hash (the record was edited)");
                }
            }
            if (previous == null && !AuditHash.GENESIS_PREV.equals(r.getPrevHash())) {
                return new Check(false, checked, r.getLogId(), "the chain does not start at the genesis record (the first records were removed)");
            }
            previous = r.getCurrHash();
            checked++;
        }
        return new Check(true, checked, 0, null);
    }
}
