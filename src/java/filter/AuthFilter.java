/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Filter.java to edit this template
 */
package filter;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import dao.FraudAlertDAO;
import dao.UserDAO;
import java.sql.SQLException;
import model.User;
import model.enums.Role;
import model.enums.UserStatus;
import service.AuthService;
import service.Features;
import model.enums.Permission;

/**
 *
 * @author PC
 */
public class AuthFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(AuthFilter.class.getName());

    private final UserDAO userDAO = new UserDAO();
    private final FraudAlertDAO fraudAlertDAO = new FraudAlertDAO();
    private final dao.AffiliateDAO affiliateDAO = new dao.AffiliateDAO();
    private final AuthService authService = new AuthService();

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = AccessPolicy.pathOf(request);

        if (AccessPolicy.isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        User user = session == null ? null : (User) session.getAttribute(LoginServlet.SESSION_USER);

        if (user == null) {
            if (AccessPolicy.isAjax(path)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        User fresh;
        try {
            fresh = userDAO.findById(user.getUserId());
        } catch (SQLException e) {
            throw new ServletException("Cannot check the account status", e);
        }
        if (fresh == null || fresh.getStatus() != UserStatus.ACTIVE) {
            session.invalidate();
            if (AccessPolicy.isAjax(path)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }
        user = authService.forSession(fresh);
        session.setAttribute(LoginServlet.SESSION_USER, user);

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");

        Permission needed = AccessPolicy.requiredFor(path, request.getMethod());
        if (needed != null && !RolePermissions.has(user.getRole(), needed)) {
            LOG.log(Level.WARNING, "ACCESS_DENIED user={0} role={1} path={2} need={3}",
                    new Object[]{user.getUserCode(), user.getRole(), path.replaceAll("[\\r\\n]", "_"), needed.getCode()});
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (AccessPolicy.isAjax(path)) {
                return;
            }
            request.setAttribute("denyReason", "You do not have the " + needed.getCode() + " permission to access this page.");
            request.getRequestDispatcher("/WEB-INF/views/common/error403.jsp").forward(request, response);
            return;
        }

        request.setAttribute("features", Features.snapshot());
        String disabled = Features.disabledModule(path);
        if (disabled != null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            if (AccessPolicy.isAjax(path)) {
                return;
            }
            request.setAttribute("disabledFeature", disabled);
            request.setAttribute("javax.servlet.error.status_code", HttpServletResponse.SC_NOT_FOUND);
            request.getRequestDispatcher("/WEB-INF/views/common/error.jsp").forward(request, response);
            return;
        }
        if (user.getRole() == Role.ADMIN && "GET".equals(request.getMethod()) && !AccessPolicy.isAjax(path)
                && !path.equals("/setup") && !path.equals("/logout") && Features.setupPending()) {
            response.sendRedirect(request.getContextPath() + "/setup");
            return;
        }

        Role who = user.getRole();
        if ((who == Role.ADMIN || who == Role.STORE_MANAGER || who == Role.CASHIER)
                && "GET".equals(request.getMethod()) && !AccessPolicy.isAjax(path)) {
            try {
                request.setAttribute("newAlertCount", fraudAlertDAO.countNew());
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "ALERT_COUNT_FAILED {0}", e.getMessage());
            }
        }

        if ("GET".equals(request.getMethod()) && !AccessPolicy.isAjax(path)) {
            try {
                request.setAttribute("myAffiliate", affiliateDAO.findForEmail(user.getEmail()));
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "AFFILIATE_LOOKUP_FAILED {0}", e.getMessage());
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
