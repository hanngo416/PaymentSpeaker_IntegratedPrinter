/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.LoginOtp;
import util.DBUtil;

/**
 *
 * @author PC
 */
public class OtpDAO {

    private static final String INSERT
            = "INSERT INTO LoginOtps (user_id, otp_hash, salt, expires_at) "
            + "VALUES (?, ?, ?, DATEADD(MINUTE, ?, SYSDATETIME()))";

    private static final String CANCEL_OLD
            = "UPDATE LoginOtps SET used_at = SYSDATETIME() WHERE user_id = ? AND used_at IS NULL";

    private static final String SELECT_ACTIVE
            = "SELECT TOP 1 otp_id, user_id, otp_hash, salt, attempts FROM LoginOtps "
            + "WHERE user_id = ? AND used_at IS NULL AND expires_at > SYSDATETIME() "
            + "ORDER BY created_at DESC";

    private static final String ADD_ATTEMPT
            = "UPDATE LoginOtps SET attempts = attempts + 1 WHERE otp_id = ?";

    private static final String MARK_USED
            = "UPDATE LoginOtps SET used_at = SYSDATETIME() WHERE otp_id = ?";

    private static final String COUNT_SINCE
            = "SELECT COUNT(*) FROM LoginOtps "
            + "WHERE user_id = ? AND created_at > DATEADD(SECOND, ?, SYSDATETIME())";

    public void insert(int userId, String otpHash, String salt, int expireMinutes) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT)) {
            ps.setInt(1, userId);
            ps.setString(2, otpHash);
            ps.setString(3, salt);
            ps.setInt(4, expireMinutes);
            ps.executeUpdate();
        }
    }

    public void cancelOld(int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(CANCEL_OLD)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public LoginOtp findActive(int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_ACTIVE)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                LoginOtp otp = new LoginOtp();
                otp.setOtpId(rs.getInt("otp_id"));
                otp.setUserId(rs.getInt("user_id"));
                otp.setOtpHash(rs.getString("otp_hash"));
                otp.setSalt(rs.getString("salt"));
                otp.setAttempts(rs.getInt("attempts"));
                return otp;
            }
        }
    }

    public void addAttempt(int otpId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(ADD_ATTEMPT)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    public void markUsed(int otpId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(MARK_USED)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    public int countSince(int userId, int seconds) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_SINCE)) {
            ps.setInt(1, userId);
            ps.setInt(2, -seconds);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
