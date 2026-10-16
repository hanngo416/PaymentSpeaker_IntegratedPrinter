package controller.system;

import controller.auth.LoginServlet;
import dao.DeviceDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Device;
import model.User;
import model.enums.Permission;
import service.AuditService;

@WebServlet(name = "SettingsServlet", urlPatterns = {"/settings"})
public class SettingsServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/system/settings.jsp";
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("maintenance", "Device is now in maintenance.");
        MESSAGES.put("online", "Device is back to work.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        try {
            request.setAttribute("devices", deviceDAO.findAll());
        } catch (SQLException e) {
            throw new ServletException("Cannot load devices", e);
        }
        request.setAttribute("canWrite", RolePermissions.has(actor.getRole(), Permission.DEVICE_WRITE));
        request.setAttribute("offlineAfter", DeviceDAO.OFFLINE_AFTER_SECONDS);
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        if (!RolePermissions.has(actor.getRole(), Permission.DEVICE_WRITE)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        String msg = "bad";
        try {
            int id = Integer.parseInt(request.getParameter("id"));
            String action = request.getParameter("action");
            Device device = deviceDAO.findById(id);
            if (device != null && ("maintenance".equals(action) || "online".equals(action))) {
                boolean maintenance = "maintenance".equals(action);
                deviceDAO.setMaintenance(id, maintenance);
                AuditService.log(actor, "DEVICE_STATUS", "DEVICE", device.getDeviceCode(),
                        "from", device.getStatus(), "to", maintenance ? "MAINTENANCE" : "ONLINE");
                msg = action;
            }
        } catch (NumberFormatException e) {
            // keeps msg = bad
        } catch (SQLException e) {
            throw new ServletException("Cannot update the device", e);
        }
        response.sendRedirect(request.getContextPath() + "/settings?msg=" + msg);
    }
}
