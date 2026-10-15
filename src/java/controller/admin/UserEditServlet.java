package controller.admin;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.enums.Role;
import service.UserService;

@WebServlet(name = "UserEditServlet", urlPatterns = {"/admin/users/edit"})
public class UserEditServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/user-form.jsp";

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        Integer id = parseInt(request.getParameter("id"));
        if (id == null && hasId(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        User u = new User();
        try {
            if (id != null) {
                u = userService.find(id);
                if (u == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if (!UserService.canManage(actor, u)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
            show(request, response, u, id == null, null, null, actor);
        } catch (SQLException e) {
            throw new ServletException("Cannot load user", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        Integer id = parseInt(request.getParameter("id"));
        if (id == null && hasId(request)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean creating = id == null;

        User u = new User();
        u.setFullName(request.getParameter("fullName"));
        u.setEmail(request.getParameter("email"));
        u.setRole(parseRole(request.getParameter("role")));

        UserService.applyScope(actor, u);

        try {
            if (!creating) {
                User current = userService.find(id);
                if (current == null) {
                    response.sendRedirect(request.getContextPath() + "/admin/users?msg=notfound");
                    return;
                }
                if (!UserService.canManage(actor, current)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                u.setUserId(id);
                u.setUserCode(current.getUserCode());
            }

            Map<String, String> errors = userService.validate(u, creating);
            if (!errors.isEmpty()) {
                show(request, response, u, creating, errors, null, actor);
                return;
            }

            if (creating) {
                userService.create(u, actor);
                response.sendRedirect(request.getContextPath() + "/admin/users?msg=created");
                return;
            }
            UserService.Result result = userService.update(u, actor);
            if (result == UserService.Result.OK) {
                response.sendRedirect(request.getContextPath() + "/admin/users?msg=updated");
            } else if (result == UserService.Result.FORBIDDEN) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            } else if (result == UserService.Result.SELF) {
                show(request, response, u, false, null, "You cannot change the role of your own account.", actor);
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/users?msg=notfound");
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot save user", e);
        }
    }

    private void show(HttpServletRequest request, HttpServletResponse response, User u,
            boolean creating, Map<String, String> errors, String formError, User actor)
            throws ServletException, IOException, SQLException {
        request.setAttribute("u", u);
        request.setAttribute("creating", creating);
        request.setAttribute("errors", errors);
        request.setAttribute("formError", formError);
        request.setAttribute("isManager", actor.getRole() == Role.STORE_MANAGER);
        request.setAttribute("roles", Role.values());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static boolean hasId(HttpServletRequest request) {
        String raw = request.getParameter("id");
        return raw != null && !raw.trim().isEmpty();
    }

    private static Role parseRole(String s) {
        try {
            return s == null || s.isEmpty() ? null : Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
