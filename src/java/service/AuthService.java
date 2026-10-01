/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.UserDAO;
import java.sql.SQLException;
import model.User;
import model.enums.UserStatus;

/**
 *
 * @author PC
 */
public class AuthService {

    public enum Status { OK, LOCKED, NOT_REGISTERED, EMAIL_NOT_VERIFIED, GOOGLE_MISMATCH }

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

    private final UserDAO userDAO = new UserDAO();

    public User findActiveUserByEmail(String email) throws SQLException {
        User user = userDAO.findByEmail(email);
        if (user == null || user.getStatus() == UserStatus.LOCKED) {
            return null;
        }
        return user;
    }

    public Result loginWithGoogle(String email, boolean emailVerified, String googleId)
            throws SQLException {
        if (!emailVerified) {
            return new Result(Status.EMAIL_NOT_VERIFIED, null);
        }
        User user = userDAO.findByEmail(email);
        if (user == null) {
            return new Result(Status.NOT_REGISTERED, null);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            return new Result(Status.LOCKED, null);
        }
        if (user.getGoogleId() == null) {
            userDAO.updateGoogleId(user.getUserId(), googleId);
        } else if (!user.getGoogleId().equals(googleId)) {
            return new Result(Status.GOOGLE_MISMATCH, null);
        }
        return new Result(Status.OK, forSession(user));
    }

    public User forSession(User user) {
        return user;
    }
}
