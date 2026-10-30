package controller.system;

import controller.auth.LoginServlet;
import dao.SettingDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.enums.Role;
import service.AuditService;
import service.Features;

@WebServlet(name = "SetupServlet", urlPatterns = {"/setup"})
public class SetupServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/system/setup.jsp";
    private static final List<String> SECTORS = Arrays.asList("GOODS", "SERVICE", "PRODUCTION", "OTHER");
    private static final Map<String, Map<String, Boolean>> PRESETS = new LinkedHashMap<>();

    static {
        PRESETS.put("LOCAL", preset(true, false, false, true));
        PRESETS.put("OMNI", preset(true, true, true, true));
        PRESETS.put("FULL", preset(true, true, true, true));
    }

    private final SettingDAO settingDAO = new SettingDAO();

    private static Map<String, Boolean> preset(boolean tax, boolean online, boolean affiliate, boolean ai) {
        Map<String, Boolean> map = new LinkedHashMap<>();
        map.put(Features.TAX, tax);
        map.put(Features.ONLINE, online);
        map.put(Features.AFFILIATE, affiliate);
        map.put(Features.AI, ai);
        return map;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        if (actor.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            request.setAttribute("taxSector", settingDAO.get("tax_sector", "GOODS"));
        } catch (SQLException e) {
            throw new ServletException("Cannot load the setup", e);
        }
        request.setAttribute("setupDone", !Features.setupPending());
        request.setAttribute("presets", PRESETS);
        request.setAttribute("sectors", SECTORS);
        request.setAttribute("message", "bad".equals(request.getParameter("msg")) ? "Choose a business sector." : null);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        if (actor.getRole() != Role.ADMIN) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        boolean skip = request.getParameter("skip") != null;
        Map<String, Boolean> chosen = new HashMap<>();
        chosen.put(Features.TAX, skip || request.getParameter("featureTax") != null);
        chosen.put(Features.ONLINE, skip || request.getParameter("featureOnline") != null);
        chosen.put(Features.AFFILIATE, skip || request.getParameter("featureAffiliate") != null);
        chosen.put(Features.AI, skip || request.getParameter("featureAi") != null);
        String sector = request.getParameter("taxSector");
        boolean sectorOk = sector != null && SECTORS.contains(sector);
        if (!skip && chosen.get(Features.TAX) && !sectorOk) {
            response.sendRedirect(request.getContextPath() + "/setup?msg=bad");
            return;
        }
        try {
            StringBuilder summary = new StringBuilder();
            for (String module : Features.MODULES) {
                settingDAO.set(Features.settingKey(module), chosen.get(module) ? "1" : "0");
                summary.append(module).append('=').append(chosen.get(module) ? "on" : "off").append(' ');
            }
            if (!skip && sectorOk && chosen.get(Features.TAX)) {
                settingDAO.set("tax_sector", sector);
            }
            settingDAO.set(Features.SETUP_DONE, "1");
            Features.invalidateCache();
            AuditService.log(actor, "SETUP_DONE", "SETTING", Features.SETUP_DONE,
                    "modules", summary.toString().trim(), "skipped", String.valueOf(skip));
        } catch (SQLException e) {
            throw new ServletException("Cannot save the setup", e);
        }
        response.sendRedirect(request.getContextPath() + "/home");
    }
}
