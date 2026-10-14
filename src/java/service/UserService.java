package service;

import dao.UserDAO;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import model.Page;
import model.User;
import model.UserFilter;
import model.enums.Role;
import model.enums.UserStatus;

public class UserService {

    public enum Result { OK, SELF, LAST_ADMIN, NOT_FOUND, HAS_HISTORY, FORBIDDEN, PENDING }

    private static final Pattern USER_CODE = Pattern.compile("^[A-Za-z0-9_.]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int SQL_FK_VIOLATION = 547;

    private static final Logger LOG = Logger.getLogger(UserService.class.getName());

    // One background thread sends the notice mails, so a slow SMTP server never blocks the manager.
    private static final ExecutorService MAIL_POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "user-notice-mail");
        t.setDaemon(true);
        return t;
    });

    private final UserDAO userDAO = new UserDAO();
    private final MailService mailService = new MailService();

    public Page<User> search(UserFilter filter) throws SQLException {
        return userDAO.search(filter);
    }

    public User find(int userId) throws SQLException {
        return userDAO.findById(userId);
    }

    public Map<String, String> validate(User u, boolean creating) throws SQLException {
        Map<String, String> errors = new LinkedHashMap<>();

        u.setFullName(trim(u.getFullName()));
        u.setEmail(trim(u.getEmail()).toLowerCase());

        if (creating) {
            u.setUserCode(trim(u.getUserCode()));
            if (!USER_CODE.matcher(u.getUserCode()).matches()) {
                errors.put("userCode", "User code must be 3-50 characters: letters, digits, dot or underscore.");
            } else if (userDAO.userCodeExists(u.getUserCode(), 0)) {
                String suggested = userDAO.suggestNextUserCode(u.getUserCode());
                errors.put("userCode", "User code '" + u.getUserCode() + "' is already taken. Suggested: '" + suggested + "'.");
            }
        }

        if (u.getFullName().isEmpty() || u.getFullName().length() > 100) {
            errors.put("fullName", "Full name is required and must be at most 100 characters.");
        }

        if (u.getEmail().length() > 256 || !EMAIL.matcher(u.getEmail()).matches()) {
            errors.put("email", "Enter a valid email address.");
        } else if (userDAO.emailExists(u.getEmail(), creating ? 0 : u.getUserId())) {
            errors.put("email", "This email is already used by another account.");
        }

        if (u.getRole() == null) {
            errors.put("role", "Choose a role.");
        }
        return errors;
    }

    /** A manager only manages CASHIER accounts; an admin manages everyone. */
    public static boolean canManage(User actor, User target) {
        if (actor.getRole() == Role.ADMIN) {
            return true;
        }
        return actor.getRole() == Role.STORE_MANAGER
                && target.getStatus() != UserStatus.PENDING
                && target.getRole() == Role.CASHIER;
    }

    /** For a manager the role comes from the session, never from the form. */
    public static void applyScope(User actor, User u) {
        if (actor.getRole() == Role.STORE_MANAGER) {
            u.setRole(Role.CASHIER);
        }
    }

    public void create(User u, User actor) throws SQLException {
        applyScope(actor, u);
        userDAO.insert(u);
        AuditService.log(actor, "CREATE_USER", "USER", u.getUserCode(), "role", String.valueOf(u.getRole()), "email", u.getEmail());
    }

    public Result update(User edited, User actor) throws SQLException {
        User current = userDAO.findById(edited.getUserId());
        if (current == null) {
            return Result.NOT_FOUND;
        }
        if (!canManage(actor, current)) {
            return Result.FORBIDDEN;
        }
        if (current.getUserId() == actor.getUserId() && current.getRole() != edited.getRole()) {
            return Result.SELF;
        }
        applyScope(actor, edited);
        boolean emailChanged = !current.getEmail().equalsIgnoreCase(edited.getEmail());
        edited.setGoogleId(emailChanged ? null : current.getGoogleId());
        userDAO.update(edited);
        AuditService.log(actor, "UPDATE_USER", "USER", current.getUserCode(),
                "role", current.getRole() + " -> " + edited.getRole(), "email", current.getEmail() + " -> " + edited.getEmail());
        notifyEdited(actor, current, edited);
        return Result.OK;
    }

    public Result setLocked(int userId, boolean lock, User actor) throws SQLException {
        User target = userDAO.findById(userId);
        if (target == null) {
            return Result.NOT_FOUND;
        }
        if (!canManage(actor, target)) {
            return Result.FORBIDDEN;
        }
        if (target.getStatus() == UserStatus.PENDING) {
            return Result.PENDING;   // approve or delete it instead; unlocking must not skip the approval
        }
        if (lock) {
            Result blocked = checkProtected(target, actor);
            if (blocked != Result.OK) {
                return blocked;
            }
        }
        userDAO.setStatus(userId, lock ? UserStatus.LOCKED : UserStatus.ACTIVE);
        AuditService.log(actor, lock ? "LOCK_USER" : "UNLOCK_USER", "USER", target.getUserCode());
        notifyLocked(actor, target, lock);
        return Result.OK;
    }

    public Result delete(int userId, User actor) throws SQLException {
        User target = userDAO.findById(userId);
        if (target == null) {
            return Result.NOT_FOUND;
        }
        if (actor.getRole() != Role.ADMIN) {
            return Result.FORBIDDEN;
        }
        Result blocked = checkProtected(target, actor);
        if (blocked != Result.OK) {
            return blocked;
        }
        try {
            userDAO.delete(userId);
            AuditService.log(actor, "DELETE_USER", "USER", target.getUserCode(), "email", target.getEmail());
            return Result.OK;
        } catch (SQLException e) {
            if (e.getErrorCode() == SQL_FK_VIOLATION) {
                return Result.HAS_HISTORY;
            }
            throw e;
        }
    }

    /** Admin gives the role to a self-registered account and opens it. */
    public Result approve(int userId, Role role, User actor) throws SQLException {
        User target = userDAO.findById(userId);
        if (target == null) {
            return Result.NOT_FOUND;
        }
        if (actor.getRole() != Role.ADMIN) {
            return Result.FORBIDDEN;
        }
        if (target.getStatus() != UserStatus.PENDING || !userDAO.approve(userId, role)) {
            return Result.NOT_FOUND;
        }
        AuditService.log(actor, "APPROVE_USER", "USER", target.getUserCode(), "role", String.valueOf(role), "email", target.getEmail());
        sendNotice(target.getEmail(), "Your account was approved",
                "Hello " + target.getFullName() + ",\n\nYour Tinky account (" + target.getUserCode()
                + ") was approved as " + role + ". You can sign in now.");
        return Result.OK;
    }

    public Result unlinkGoogle(int userId, User actor) throws SQLException {
        User target = userDAO.findById(userId);
        if (target == null) {
            return Result.NOT_FOUND;
        }
        userDAO.clearGoogleId(userId);
        AuditService.log(actor, "UNLINK_GOOGLE", "USER", target.getUserCode());
        return Result.OK;
    }

    // Notice mails: only when a manager changes a cashier. Admin changes send nothing.
    private void notifyEdited(User actor, User before, User after) {
        if (actor.getRole() != Role.STORE_MANAGER) {
            return;
        }
        StringBuilder changes = new StringBuilder();
        if (!before.getFullName().equals(after.getFullName())) {
            changes.append("- Full name: ").append(before.getFullName()).append(" -> ").append(after.getFullName()).append("\n");
        }
        boolean emailChanged = !before.getEmail().equalsIgnoreCase(after.getEmail());
        if (emailChanged) {
            changes.append("- Login email: ").append(before.getEmail()).append(" -> ").append(after.getEmail()).append("\n");
            changes.append("- Your linked Google account was removed. Link it again at your next Google login.\n");
        }
        if (changes.length() == 0) {
            return;
        }
        String body = "Hello " + after.getFullName() + ",\n\n"
                + "Your Tinky account (" + before.getUserCode() + ") was updated by "
                + actor.getFullName() + " (" + actor.getUserCode() + "):\n\n"
                + changes + "\nIf you did not expect this change, contact your manager.";
        sendNotice(after.getEmail(), "Your account information was changed", body);
        if (emailChanged) {
            sendNotice(before.getEmail(), "Your account information was changed", body);
        }
    }

    private void notifyLocked(User actor, User target, boolean locked) {
        if (actor.getRole() != Role.STORE_MANAGER) {
            return;
        }
        String body = "Hello " + target.getFullName() + ",\n\n"
                + "Your Tinky account (" + target.getUserCode() + ") was "
                + (locked ? "locked" : "unlocked") + " by " + actor.getFullName()
                + " (" + actor.getUserCode() + ").\n\n"
                + (locked ? "You cannot sign in until your manager unlocks it."
                          : "You can sign in again.");
        sendNotice(target.getEmail(), locked ? "Your account was locked" : "Your account was unlocked", body);
    }

    private void sendNotice(String to, String subject, String body) {
        MAIL_POOL.submit(() -> {
            try {
                mailService.send(to, subject, body);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cannot send the notice mail", e);
            }
        });
    }

    private Result checkProtected(User target, User actor) throws SQLException {
        if (target.getUserId() == actor.getUserId()) {
            return Result.SELF;
        }
        if (target.getRole() == Role.ADMIN && target.getStatus() == UserStatus.ACTIVE
                && userDAO.countActiveAdmins() <= 1) {
            return Result.LAST_ADMIN;
        }
        return Result.OK;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
