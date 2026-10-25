package controller.bot;

import controller.auth.LoginServlet;
import dao.FraudAlertDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.enums.Permission;
import model.view.TaxEstimate;
import service.TaxService;

@WebServlet(name = "BotFeedServlet", urlPatterns = {"/bot/feed"})
public class BotFeedServlet extends HttpServlet {

    private final TaxService taxService = new TaxService();
    private final FraudAlertDAO alertDAO = new FraudAlertDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        int year = LocalDate.now().getYear();
        TaxService.TaxView view;
        int newAlerts = 0;
        try {
            taxService.notifyIfLevelRose(year);
            view = taxService.view(year);
            if (RolePermissions.has(user.getRole(), Permission.ALERT_READ)) {
                newAlerts = alertDAO.countNew();
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot build the bot feed", e);
        }

        TaxEstimate e = view.getEstimate();
        int level = e == null ? 0 : e.getLevel();
        String percent = e == null ? "0.00" : e.getPercent().toPlainString();
        String remaining = e == null ? "0" : e.getRemaining().toBigInteger().toString();

        List<String> messages = new ArrayList<>();
        if (e != null && level >= 1) {
            String key = "tax-" + year + "-level" + level;
            String text;
            String label;
            if (level >= 3) {
                text = "Revenue for " + year + " has reached or passed the tax-free threshold ("
                        + e.getPercent().toPlainString() + "%). Check what to do next.";
                label = "See what to do";
            } else {
                text = "Revenue for " + year + " is at " + e.getPercent().toPlainString()
                        + "% of the tax-free threshold. " + money(remaining) + " VND left before it.";
                label = "Open tax estimate";
            }
            messages.add(message(key, text, "/tax#todo", label));
        }
        if (newAlerts > 0) {
            messages.add(message("alerts-new-" + newAlerts,
                    newAlerts + " new alert(s) need a look.", "/alerts", "Open alerts"));
        }

        StringBuilder json = new StringBuilder("{\"level\":").append(level)
                .append(",\"percent\":\"").append(percent)
                .append("\",\"remaining\":\"").append(remaining)
                .append("\",\"newAlerts\":").append(newAlerts)
                .append(",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append(messages.get(i));
        }
        json.append("]}");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json.toString());
    }

    private static String message(String key, String text, String link, String label) {
        return "{\"key\":\"" + escape(key) + "\",\"text\":\"" + escape(text)
                + "\",\"link\":\"" + escape(link) + "\",\"label\":\"" + escape(label) + "\"}";
    }

    private static String money(String digits) {
        return String.format("%,d", Long.parseLong(digits));
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
