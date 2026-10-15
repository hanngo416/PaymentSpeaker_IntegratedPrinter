package controller.shift;

import controller.auth.LoginServlet;
import dao.DeviceDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.enums.Permission;
import model.enums.Role;
import model.view.Report;
import model.view.ShiftRow;
import service.AuditService;
import service.ReportService;
import service.ShiftService;

/**
 * The counter shift: /shift opens and closes it, /shift/print prints the summary of one shift or of a whole day.
 */
@WebServlet(name = "ShiftServlet", urlPatterns = {"/shift", "/shift/print"})
public class ShiftServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/shift/shift.jsp";
    private static final String PRINT = "/WEB-INF/views/shift/report-print.jsp";
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("opened", "Shift started. You can create orders now.");
        MESSAGES.put("noshift", "Start a shift before creating orders.");
        MESSAGES.put("already", "A shift is already open.");
        MESSAGES.put("none", "No shift is open.");
        MESSAGES.put("cash", "Enter the cash counted in the drawer (digits only, 0 if empty).");
        MESSAGES.put("busy", "The printer is in maintenance, nothing can be printed now.");
        MESSAGES.put("missing", "That shift or day was not found.");
    }

    private final ShiftService shiftService = new ShiftService();
    private final ReportService reportService = new ReportService();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        try {
            if ("/shift/print".equals(request.getServletPath())) {
                print(request, response, actor);
                return;
            }
            ShiftRow open = shiftService.current();
            request.setAttribute("shift", open);
            if (open != null) {
                request.setAttribute("report", reportService.forShift(open.getId()));
            }
            request.setAttribute("recent", reportService.recentShifts(10));
            request.setAttribute("today", LocalDate.now().toString());
            request.setAttribute("isAdmin", actor.getRole() == Role.ADMIN);
            request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        } catch (SQLException e) {
            throw new ServletException("Cannot load the shift", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/shift".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        String ctx = request.getContextPath();
        try {
            if ("open".equals(action)) {
                shiftService.open(actor);
                response.sendRedirect(ctx + "/shift?msg=opened");
            } else if ("close".equals(action) || "force".equals(action)) {
                boolean force = "force".equals(action);
                if (force && actor.getRole() != Role.ADMIN) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                ShiftRow closed = shiftService.close(actor, request.getParameter("counted"), force);
                response.sendRedirect(ctx + "/shift/print?id=" + closed.getId());
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        } catch (IllegalStateException e) {
            response.sendRedirect(ctx + "/shift?msg=" + (ShiftService.ALREADY_OPEN.equals(e.getMessage()) ? "already" : "none"));
        } catch (IllegalArgumentException e) {
            response.sendRedirect(ctx + "/shift?msg=cash");
        } catch (SQLException e) {
            throw new ServletException("Cannot save the shift", e);
        }
    }

    private void print(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        String ctx = request.getContextPath();
        // Auditors have no shift page, they go back to the statistics page
        String back = RolePermissions.has(actor.getRole(), Permission.SHIFT_USE) ? "/shift" : "/invoices";
        if (deviceDAO.isInMaintenance("PRINTER")) {
            response.sendRedirect(ctx + back + "?msg=busy");
            return;
        }
        String id = request.getParameter("id");
        String date = request.getParameter("date");
        Report report = null;
        String key = null;
        if (id != null && id.matches("S\\d{8}-\\d{6}")) {
            report = reportService.forShift(id);
            key = id;
        } else if (date != null) {
            try {
                LocalDate day = LocalDate.parse(date);
                report = reportService.forDay(day);
                key = day.toString();
            } catch (DateTimeParseException e) {
                report = null;
            }
        }
        if (report == null) {
            response.sendRedirect(ctx + back + "?msg=missing");
            return;
        }
        AuditService.log(actor, "PRINT_REPORT", "SHIFT", key, "title", report.getTitle());
        request.setAttribute("report", report);
        request.setAttribute("printedBy", actor.getUserCode());
        request.setAttribute("printedAt", new Date());
        request.getRequestDispatcher(PRINT).forward(request, response);
    }
}
