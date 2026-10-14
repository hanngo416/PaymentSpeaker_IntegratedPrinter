package service;

import com.google.gson.JsonObject;
import dao.AuditDAO;
import dao.ShiftDAO;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import model.User;
import model.view.ShiftRow;

/**
 * Opening and closing the counter. One shift is open at a time for the whole store; the pair OPEN_SHIFT and CLOSE_SHIFT
 * in the audit log is the shift. These records are written directly (not through AuditService) so a failed write is an
 * error the cashier sees instead of a shift that silently never started.
 */
public class ShiftService {

    public static final String ALREADY_OPEN = "A shift is already open.";
    public static final String NOT_OPEN = "No shift is open.";

    private static final AuditDAO AUDIT = new AuditDAO();

    private final ShiftDAO shiftDAO = new ShiftDAO();

    public ShiftRow current() throws SQLException {
        return shiftDAO.findOpen();
    }

    public synchronized ShiftRow open(User user) throws SQLException {
        if (shiftDAO.findOpen() != null) {
            throw new IllegalStateException(ALREADY_OPEN);
        }
        String id = "S" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        AUDIT.append(user.getUserCode(), ShiftDAO.OPEN, "SHIFT", id, "{}");
        return shiftDAO.find(id);
    }

    // The cashier types the cash counted in the drawer; an admin can close a forgotten shift without a count (force)
    public synchronized ShiftRow close(User user, String rawCounted, boolean force) throws SQLException {
        ShiftRow open = shiftDAO.findOpen();
        if (open == null) {
            throw new IllegalStateException(NOT_OPEN);
        }
        JsonObject detail = new JsonObject();
        if (force) {
            detail.addProperty("forced", "true");
        } else {
            detail.addProperty("counted_cash", String.valueOf(parseCash(rawCounted)));
        }
        AUDIT.append(user.getUserCode(), ShiftDAO.CLOSE, "SHIFT", open.getId(), detail.toString());
        return shiftDAO.find(open.getId());
    }

    static long parseCash(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[.,\\s]", "");
        if (!digits.matches("\\d{1,12}")) {
            throw new IllegalArgumentException("Enter the cash counted in the drawer (digits only, 0 if empty).");
        }
        return Long.parseLong(digits);
    }
}
