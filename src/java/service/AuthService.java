/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.UserDAO;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.User;
import model.enums.UserStatus;

/**
 *
 * @author PC
 */
public class AuthService {

    public enum Status { OK, LOCKED, PENDING, EMAIL_NOT_VERIFIED, GOOGLE_MISMATCH }

    public static class Result {
        private final Status status;
        private final User user;

        private Result(Status status, User user) {
            this.status = status;
            this.user = user;
        }

        public Status getStatus() { return status; }
        public User getUser() { return user; }
    }

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());
    private static final int SQL_DUPLICATE_KEY = 2627;
    private static final int SQL_DUPLICATE_INDEX = 2601;

    private final UserDAO userDAO = new UserDAO();

    public User findActiveUserByEmail(String email) throws SQLException {
        User user = userDAO.findByEmail(email);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        return user;
    }

    public Result loginWithGoogle(String email, String fullName, boolean emailVerified, String googleId)
            throws SQLException {
        if (!emailVerified) {
            return new Result(Status.EMAIL_NOT_VERIFIED, null);
        }
        User user = userDAO.findByEmail(email);
        if (user == null) {
            user = register(email, fullName, googleId);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            return new Result(Status.LOCKED, null);
        }
        if (user.getGoogleId() == null) {
            userDAO.updateGoogleId(user.getUserId(), googleId);
        } else if (!user.getGoogleId().equals(googleId)) {
            return new Result(Status.GOOGLE_MISMATCH, null);
        }
        if (user.getStatus() == UserStatus.PENDING) {
            return new Result(Status.PENDING, null);
        }
        return new Result(Status.OK, forSession(user));
    }

    // First Google sign-in of an unknown email: open a PENDING account that an admin must approve
    private User register(String email, String fullName, String googleId) throws SQLException {
        String name = fullName == null || fullName.trim().isEmpty() ? email : fullName.trim();
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }
        String code = userCodeFrom(email);
        if (userDAO.userCodeExists(code, 0)) {
            code = userDAO.suggestNextUserCode(code);
        }
        try {
            userDAO.insertPending(code, name, email, googleId);
            AuditService.log(null, "REGISTER_USER", "USER", code, "email", email);
        } catch (SQLException e) {
            // Two sign-ins at the same moment: the other one created it first, so use that account
            if (e.getErrorCode() != SQL_DUPLICATE_KEY && e.getErrorCode() != SQL_DUPLICATE_INDEX) {
                throw e;
            }
            LOG.log(Level.INFO, "REGISTER_RACE email={0}", email);
        }
        return userDAO.findByEmail(email);
    }

    // User code = the part of the email before @, keeping only letters, digits, dot and underscore (3-50 characters)
    private static String userCodeFrom(String email) {
        String local = email.substring(0, Math.max(email.indexOf('@'), 0)).replaceAll("[^A-Za-z0-9_.]", "");
        if (local.length() < 3) {
            local = "user";
        }
        return local.length() > 50 ? local.substring(0, 50) : local;
    }

    public User forSession(User user) {
        return user;
    }
}
