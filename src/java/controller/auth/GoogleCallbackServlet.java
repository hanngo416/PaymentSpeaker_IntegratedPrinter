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
import service.AuthService;
import service.GoogleOAuthService;

/**
 *
 * @author PC
 */
@WebServlet(name = "GoogleCallbackServlet", urlPatterns = {"/login/google/callback"})
public class GoogleCallbackServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(GoogleCallbackServlet.class.getName());

    private final AuthService authService = new AuthService();
    private final GoogleOAuthService googleService = new GoogleOAuthService();

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
        String code = request.getParameter("code");
        String state = request.getParameter("state");

        String expectedState = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            expectedState = (String) session.getAttribute(GoogleLoginServlet.SESSION_OAUTH_STATE);
            session.removeAttribute(GoogleLoginServlet.SESSION_OAUTH_STATE);
        }

        if (request.getParameter("error") != null || code == null) {
            showLogin(request, response, "Google sign-in was cancelled.");
            return;
        }
        if (expectedState == null || !expectedState.equals(state)) {
            showLogin(request, response, "The sign-in request is invalid or has expired. Please try again.");
            return;
        }

        AuthService.Result result;
        try {
            GoogleOAuthService.GoogleProfile profile = googleService.fetchProfile(code);
            result = authService.loginWithGoogle(
                    profile.getEmail(), profile.getName(), profile.isEmailVerified(), profile.getGoogleId());
        } catch (IOException | SQLException e) {
            LOG.log(Level.SEVERE, "Google login failed", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            showLogin(request, response, "The system is having a problem. Please try again later.");
            return;
        }

        AuthService.Status status = result.getStatus();
        if (status == AuthService.Status.OK) {
            LOG.log(Level.INFO, "GOOGLE_LOGIN_OK user={0} ip={1}",
                    new Object[]{result.getUser().getUserCode(), request.getRemoteAddr()});
            LoginServlet.startSession(request, result.getUser());
            response.sendRedirect(request.getContextPath() + "/home");
        } else if (status == AuthService.Status.NOT_REGISTERED) {
            showLogin(request, response, "This Google account is not registered. Please contact an administrator.");
        } else if (status == AuthService.Status.PENDING) {
            showLogin(request, response, "Your account is waiting for an administrator to approve it and give it a role. Please try again later.");
        } else if (status == AuthService.Status.LOCKED) {
            showLogin(request, response, "This account is locked. Please contact an administrator.");
        } else if (status == AuthService.Status.EMAIL_NOT_VERIFIED) {
            showLogin(request, response, "Your Google email is not verified.");
        } else {
            showLogin(request, response, "This account is linked to a different Google account. Please contact an administrator.");
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

    private void showLogin(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        HttpSession session = request.getSession(true);
        session.setAttribute("loginError", error);
        response.sendRedirect(request.getContextPath() + "/login");
    }
}
