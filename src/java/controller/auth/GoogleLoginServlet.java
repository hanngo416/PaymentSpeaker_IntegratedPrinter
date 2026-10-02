/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.auth;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.GoogleOAuthService;

/**
 *
 * @author PC
 */
@WebServlet(name = "GoogleLoginServlet", urlPatterns = {"/login/google"})
public class GoogleLoginServlet extends HttpServlet {

    public static final String SESSION_OAUTH_STATE = "oauthState";

    private static final SecureRandom RANDOM = new SecureRandom();

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
        String state = new BigInteger(130, RANDOM).toString(32);
        request.getSession(true).setAttribute(SESSION_OAUTH_STATE, state);
        response.sendRedirect(googleService.buildLoginUrl(state));
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
}
