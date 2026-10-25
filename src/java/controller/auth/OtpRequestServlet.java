/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.auth;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import service.AuthService;
import service.OtpService;
import static util.Params.trim;

/**
 *
 * @author PC
 */
@WebServlet(name = "OtpRequestServlet", urlPatterns = {"/login/otp"})
public class OtpRequestServlet extends HttpServlet {

    public static final String SESSION_OTP_EMAIL = "otpEmail";
    private static final String LOGIN_VIEW = "/WEB-INF/views/auth/login.jsp";
    private static final Logger LOG = Logger.getLogger(OtpRequestServlet.class.getName());

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
        response.sendRedirect(request.getContextPath() + "/login");
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
        String email = trim(request.getParameter("email")).toLowerCase();

        if (!email.contains("@")) {
            showLogin(request, response, email, "Please enter a valid email address.");
            return;
        }

        try {
            User user = authService.findActiveUserByEmail(email);
            if (user != null) {
                boolean sent = otpService.sendOtp(user);
                LOG.log(Level.INFO, "OTP_REQUEST user={0} sent={1} ip={2}",
                        new Object[]{user.getUserCode(), sent, request.getRemoteAddr()});
            }
        } catch (SQLException | MessagingException e) {
            LOG.log(Level.SEVERE, "Cannot send the login code", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            showLogin(request, response, email, "The system is having a problem. Please try again later.");
            return;
        }

        request.getSession(true).setAttribute(SESSION_OTP_EMAIL, email);
        response.sendRedirect(request.getContextPath() + "/login/verify");
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

    private void showLogin(HttpServletRequest request, HttpServletResponse response,
            String email, String error) throws ServletException, IOException {
        request.setAttribute("error", error);
        request.setAttribute("email", email);
        request.getRequestDispatcher(LOGIN_VIEW).forward(request, response);
    }
}
