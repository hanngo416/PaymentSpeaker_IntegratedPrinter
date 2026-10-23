package service;

import com.google.gson.JsonObject;
import dao.AuditDAO;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.User;

public class AuditService {

    public static final String SYSTEM = "SYSTEM";

    private static final Logger LOG = Logger.getLogger(AuditService.class.getName());
    private static final AuditDAO DAO = new AuditDAO();

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
}
