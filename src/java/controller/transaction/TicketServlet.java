package controller.transaction;

import controller.auth.LoginServlet;
import dao.DeviceDAO;
import dao.TicketDAO;
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
import model.User;
import model.enums.Permission;
import service.AuditService;

/**
 * Hardware incident tickets. Anyone who works at the counter reports a problem; the technician takes it and closes it.
 */
@WebServlet(name = "TicketServlet", urlPatterns = {"/tickets"})
public class TicketServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/transaction/tickets.jsp";
    private static final List<String> STATUSES = Arrays.asList("OPEN", "IN_PROGRESS", "RESOLVED");
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("created", "Ticket created. A technician will take it.");
        MESSAGES.put("updated", "Ticket updated.");
        MESSAGES.put("stale", "This ticket is already resolved.");
        MESSAGES.put("desc", "Describe the problem (up to 500 characters).");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final TicketDAO ticketDAO = new TicketDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Maintenance tickets are switched off for now (every role, admin included); delete these 3 lines to turn them on again
        if (true) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String status = request.getParameter("status");
        String filter = STATUSES.contains(status) ? status : null;
        try {
            request.setAttribute("tickets", ticketDAO.search(filter));
            request.setAttribute("devices", deviceDAO.findAll());
        } catch (SQLException e) {
            throw new ServletException("Cannot load tickets", e);
        }
        request.setAttribute("statuses", STATUSES);
        request.setAttribute("status", filter);
        request.setAttribute("canHandle", RolePermissions.has(actor.getRole(), Permission.TICKET_HANDLE));
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Maintenance tickets are switched off for now (every role, admin included); delete these 3 lines to turn them on again
        if (true) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        String msg = "bad";
        try {
            if ("create".equals(action)) {
                msg = create(request, actor);
            } else if ("update".equals(action)) {
                if (!RolePermissions.has(actor.getRole(), Permission.TICKET_HANDLE)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                msg = update(request, actor);
            }
        } catch (NumberFormatException e) {
            // keeps msg = bad
        } catch (SQLException e) {
            throw new ServletException("Cannot save the ticket", e);
        }
        response.sendRedirect(request.getContextPath() + "/tickets?msg=" + msg);
    }

    private String create(HttpServletRequest request, User actor) throws SQLException {
        int deviceId = Integer.parseInt(request.getParameter("deviceId"));
        String description = trim(request.getParameter("description"));
        if (description.isEmpty() || description.length() > 500) {
            return "desc";
        }
        if (deviceDAO.findById(deviceId) == null) {
            return "bad";
        }
        int id = ticketDAO.create(deviceId, actor.getUserId(), description);
        AuditService.log(actor, "CREATE_TICKET", "TICKET", id, "deviceId", String.valueOf(deviceId), "issue", description);
        return "created";
    }

    private String update(HttpServletRequest request, User actor) throws SQLException {
        int id = Integer.parseInt(request.getParameter("id"));
        String status = request.getParameter("status");
        if (!"IN_PROGRESS".equals(status) && !"RESOLVED".equals(status)) {
            return "bad";
        }
        String notes = trim(request.getParameter("notes"));
        if (notes.length() > 500) {
            notes = notes.substring(0, 500);
        }
        if (!ticketDAO.update(id, status, notes)) {
            return "stale";
        }
        AuditService.log(actor, "UPDATE_TICKET", "TICKET", id, "status", status, "notes", notes);
        return "updated";
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
