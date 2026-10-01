/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.OtpDAO;
import java.security.SecureRandom;
import java.sql.SQLException;
import javax.mail.MessagingException;
import model.LoginOtp;
import model.User;
import util.PasswordUtil;

/**
 *
 * @author PC
 */
public class OtpService {

    public static final int EXPIRE_MINUTES = 5;
    public static final int MAX_WRONG_TRIES = 5;
    public static final int RESEND_GAP_SECONDS = 60;
    public static final int MAX_PER_HOUR = 5;

    public enum VerifyResult { OK, WRONG_CODE, EXPIRED_OR_MISSING, TOO_MANY_TRIES }

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpDAO otpDAO = new OtpDAO();
    private final MailService mailService = new MailService();

    public boolean sendOtp(User user) throws SQLException, MessagingException {
        if (otpDAO.countSince(user.getUserId(), RESEND_GAP_SECONDS) > 0) {
            return false;
        }
        if (otpDAO.countSince(user.getUserId(), 3600) >= MAX_PER_HOUR) {
            return false;
        }

        String code = String.format("%06d", RANDOM.nextInt(1000000));
        String salt = PasswordUtil.newSalt();

        otpDAO.cancelOld(user.getUserId());
        otpDAO.insert(user.getUserId(), PasswordUtil.hash(code, salt), salt, EXPIRE_MINUTES);

        String body = "Your Tinky login code is: " + code + "\n\n"
                + "It expires in " + EXPIRE_MINUTES + " minutes. "
                + "If you did not ask for it, you can ignore this email.";
        mailService.send(user.getEmail(), "Your login code", body);
        return true;
    }

    public VerifyResult verify(User user, String code) throws SQLException {
        LoginOtp otp = otpDAO.findActive(user.getUserId());
        if (otp == null) {
            return VerifyResult.EXPIRED_OR_MISSING;
        }
        if (otp.getAttempts() >= MAX_WRONG_TRIES) {
            return VerifyResult.TOO_MANY_TRIES;
        }
        if (!PasswordUtil.verify(code, otp.getSalt(), otp.getOtpHash())) {
            otpDAO.addAttempt(otp.getOtpId());
            return VerifyResult.WRONG_CODE;
        }
        otpDAO.markUsed(otp.getOtpId());
        return VerifyResult.OK;
    }
}
