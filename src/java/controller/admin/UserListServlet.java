package controller.admin;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.User;
import model.UserFilter;
import model.enums.Permission;
import model.enums.Role;
import model.enums.UserStatus;
import filter.RolePermissions;
import service.UserService;
import static util.Params.parseInt;

@WebServlet(name = "UserListServlet", urlPatterns = {"/admin/users"})
public class UserListServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/users.jsp";
    private static final List<String> SORTS = Arrays.asList("usercode", "name", "role", "status");
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("created", "User created.");
        MESSAGES.put("updated", "User updated.");
        MESSAGES.put("locked", "Account locked. The user is signed out on the next request.");
        MESSAGES.put("unlocked", "Account unlocked.");
        MESSAGES.put("deleted", "User deleted.");
        MESSAGES.put("approved", "Account approved. The user can sign in now.");
        MESSAGES.put("pending", "This account is waiting for approval. Approve or delete it instead.");
        MESSAGES.put("unlinked", "Google account unlinked. The user can link again at the next Google login.");
        MESSAGES.put("self", "You cannot lock, delete or change the role of your own account.");
        MESSAGES.put("lastadmin", "The last active administrator cannot be locked or deleted.");
        MESSAGES.put("history", "This user has history (orders) and cannot be deleted. Lock the account instead.");
        MESSAGES.put("notfound", "User not found.");
    }

    private final UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);

        UserFilter f = new UserFilter();
        f.setKeyword(trim(request.getParameter("q")));
        f.setRole(oneOf(request.getParameter("role"), enumNames(Role.values())));
        f.setStatus(oneOf(request.getParameter("status"), enumNames(UserStatus.values())));
        String sort = request.getParameter("sort");
        f.setSort(SORTS.contains(sort) ? sort : "usercode");
        f.setDir("desc".equals(request.getParameter("dir")) ? "desc" : "asc");
        Integer page = parseInt(request.getParameter("page"));
        f.setPage(page == null || page < 1 ? 1 : page);

        if (actor.getRole() != Role.ADMIN) {
            f.setRole(Role.CASHIER.name());
        }

        try {
            Page<User> result = userService.search(f);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                f.setPage(result.getTotalPages());
                result = userService.search(f);
            }
            request.setAttribute("result", result);
            request.setAttribute("f", f);
            request.setAttribute("roles", Role.values());
            request.setAttribute("statuses", UserStatus.values());
            request.setAttribute("canWrite", RolePermissions.has(actor.getRole(), Permission.USER_WRITE));
            request.setAttribute("isAdmin", actor.getRole() == Role.ADMIN);
            request.setAttribute("filterQuery", filterQuery(f));
            if (actor.getRole() == Role.ADMIN) {
                UserFilter waiting = new UserFilter();
                waiting.setStatus(UserStatus.PENDING.name());
                waiting.setSort("usercode");
                waiting.setDir("asc");
                waiting.setPage(1);
                request.setAttribute("pendingCount", userService.search(waiting).getTotal());
            }
            request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        } catch (SQLException e) {
            throw new ServletException("Cannot load users", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String filterQuery(UserFilter f) {
        StringBuilder q = new StringBuilder();
        append(q, "q", f.getKeyword());
        append(q, "role", f.getRole());
        append(q, "status", f.getStatus());
        return q.toString();
    }

    private static void append(StringBuilder q, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            q.append(name).append('=').append(URLEncoder.encode(value, "UTF-8")).append('&');
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String[] enumNames(Enum<?>[] values) {
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].name();
        }
        return names;
    }

    private static String oneOf(String value, String[] allowed) {
        return value != null && Arrays.asList(allowed).contains(value) ? value : null;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
