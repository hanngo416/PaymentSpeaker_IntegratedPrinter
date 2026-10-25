package controller.transaction;

import controller.auth.LoginServlet;
import dao.FraudAlertDAO;
import filter.RolePermissions;
import java.io.IOException;
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
import model.AlertFilter;
import model.Page;
import model.User;
import model.enums.Permission;
import model.view.AlertRow;
import service.AuditService;
import static util.Params.parseInt;

@WebServlet(name = "AlertServlet", urlPatterns = {"/alerts"})
public class AlertServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/transaction/alerts.jsp";
    private static final List<String> STATUSES = Arrays.asList("NEW", "IN_PROGRESS", "RESOLVED", "DISMISSED");
    private static final List<String> SEVERITIES = Arrays.asList("LOW", "MEDIUM", "HIGH");
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("resolved", "Alert marked as resolved.");
        MESSAGES.put("dismissed", "Alert dismissed.");
        MESSAGES.put("stale", "This alert was already handled.");
        MESSAGES.put("started", "Alert is now in progress.");
        MESSAGES.put("note", "Enter the reason for dismissing the alert.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final FraudAlertDAO alertDAO = new FraudAlertDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);

        AlertFilter f = new AlertFilter();
        String status = request.getParameter("status");
        String severity = request.getParameter("severity");
        f.setStatus(STATUSES.contains(status) ? status : null);
        f.setSeverity(SEVERITIES.contains(severity) ? severity : null);
        Integer page = parseInt(request.getParameter("page"));
        f.setPage(page == null || page < 1 ? 1 : page);

        try {
            Page<AlertRow> result = alertDAO.search(f);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                f.setPage(result.getTotalPages());
                result = alertDAO.search(f);
            }
            request.setAttribute("result", result);
            request.setAttribute("f", f);
            request.setAttribute("statuses", STATUSES);
            request.setAttribute("severities", SEVERITIES);
            request.setAttribute("counts", alertDAO.countByStatus());
            request.setAttribute("canWrite", RolePermissions.has(actor.getRole(), Permission.ALERT_WRITE));
            request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        } catch (SQLException e) {
            throw new ServletException("Cannot load alerts", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        Integer id = parseInt(request.getParameter("id"));
        String decision = request.getParameter("decision");
        String notes = request.getParameter("notes");
        notes = notes == null ? "" : notes.trim();
        if (notes.length() > 500) {
            notes = notes.substring(0, 500);
        }

        String msg;
        if (id == null || !("RESOLVED".equals(decision) || "DISMISSED".equals(decision) || "IN_PROGRESS".equals(decision))) {
            msg = "bad";
        } else if ("DISMISSED".equals(decision) && notes.isEmpty()) {
            msg = "note";
        } else if ("IN_PROGRESS".equals(decision)) {
            try {
                boolean started = alertDAO.startReview(id, actor.getUserId());
                if (started) {
                    AuditService.log(actor, "ALERT_START_REVIEW", "ALERT", id);
                }
                msg = started ? "started" : "stale";
            } catch (SQLException e) {
                throw new ServletException("Cannot update alert", e);
            }
        } else {
            try {
                boolean changed = alertDAO.updateStatus(id, decision, actor.getUserId(), notes);
                if (changed) {
                    AuditService.log(actor, "RESOLVED".equals(decision) ? "ALERT_RESOLVE" : "ALERT_DISMISS", "ALERT", id, "notes", notes);
                }
                msg = !changed ? "stale" : "RESOLVED".equals(decision) ? "resolved" : "dismissed";
            } catch (SQLException e) {
                throw new ServletException("Cannot update alert", e);
            }
        }
        response.sendRedirect(request.getContextPath() + "/alerts?msg=" + msg);
    }
}
