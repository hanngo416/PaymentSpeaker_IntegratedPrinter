/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.auth;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.User;
import service.AuthService;
import service.OtpService;

/**
 *
 * @author PC
 */
@WebServlet(name = "OtpVerifyServlet", urlPatterns = {"/login/verify"})
public class OtpVerifyServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/auth/verify-otp.jsp";
    private static final Logger LOG = Logger.getLogger(OtpVerifyServlet.class.getName());

    private final AuthService authService = new AuthService();
    private final OtpService otpService = new OtpService();

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (pendingEmail(request) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = pendingEmail(request);
        if (email == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String code = request.getParameter("code") == null ? "" : request.getParameter("code").trim();
        if (!code.matches("\\d{6}")) {
            showForm(request, response, "Please enter the 6-digit code.");
            return;
        }

        try {
            User user = authService.findActiveUserByEmail(email);
            OtpService.VerifyResult result = (user == null)
                    ? OtpService.VerifyResult.EXPIRED_OR_MISSING
                    : otpService.verify(user, code);

            switch (result) {
                case OK:
                    LOG.log(Level.INFO, "OTP_LOGIN_OK user={0} ip={1}",
                            new Object[]{user.getUserCode(), request.getRemoteAddr()});
                    LoginServlet.startSession(request, authService.forSession(user));
                    response.sendRedirect(request.getContextPath() + "/home");
                    return;
                case WRONG_CODE:
                    showForm(request, response, "Wrong code. Please try again.");
                    return;
                case TOO_MANY_TRIES:
                    showForm(request, response, "Too many wrong tries. Please go back and request a new code.");
                    return;
                default:
                    showForm(request, response, "The code is invalid or has expired. Please go back and request a new code.");
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error while checking the login code", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            showForm(request, response, "The system is having a problem. Please try again later.");
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    private static String pendingEmail(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(OtpRequestServlet.SESSION_OTP_EMAIL);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("error", error);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
