package controller.admin;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.enums.Role;
import service.UserService;

@WebServlet(name = "UserActionServlet", urlPatterns = {"/admin/users/action"})
public class UserActionServlet extends HttpServlet {

    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        String msg;
        try {
            UserService.Result result;
            if (("delete".equals(action) || "unlink".equals(action)) && actor.getRole() != Role.ADMIN) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if ("lock".equals(action)) {
                result = userService.setLocked(id, true, actor);
                if (result == UserService.Result.FORBIDDEN) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                msg = result == UserService.Result.OK ? "locked" : failure(result);
            } else if ("unlock".equals(action)) {
                result = userService.setLocked(id, false, actor);
                if (result == UserService.Result.FORBIDDEN) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                msg = result == UserService.Result.OK ? "unlocked" : failure(result);
            } else if ("approve".equals(action)) {
                Role role = parseRole(request.getParameter("role"));
                if (role == null) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
                result = userService.approve(id, role, actor);
                if (result == UserService.Result.FORBIDDEN) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                msg = result == UserService.Result.OK ? "approved" : failure(result);
            } else if ("delete".equals(action)) {
                result = userService.delete(id, actor);
                if (result == UserService.Result.FORBIDDEN) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                msg = result == UserService.Result.OK ? "deleted" : failure(result);
            } else if ("unlink".equals(action)) {
                result = userService.unlinkGoogle(id, actor);
                msg = result == UserService.Result.OK ? "unlinked" : failure(result);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot run user action", e);
        }
        response.sendRedirect(request.getContextPath() + "/admin/users?msg=" + msg);
    }

    private static Role parseRole(String s) {
        try {
            return s == null ? null : Role.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String failure(UserService.Result result) {
        if (result == UserService.Result.SELF) {
            return "self";
        }
        if (result == UserService.Result.LAST_ADMIN) {
            return "lastadmin";
        }
        if (result == UserService.Result.HAS_HISTORY) {
            return "history";
        }
        if (result == UserService.Result.PENDING) {
            return "pending";
        }
        return "notfound";
    }
}
