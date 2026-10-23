package controller.affiliate;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;

public abstract class AffiliateBase extends HttpServlet {

    protected static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("saved", "Saved.");
        MESSAGES.put("created", "Created.");
        MESSAGES.put("deleted", "Deleted.");
        MESSAGES.put("used", "This record is already in use and cannot be deleted.");
        MESSAGES.put("bad", "Invalid request.");
        MESSAGES.put("missing", "Record not found.");
        MESSAGES.put("blocked", "That change is not allowed in the current status.");
        MESSAGES.put("approved", "Commission approved.");
        MESSAGES.put("rejected", "Commission rejected.");
        MESSAGES.put("held", "Commission put on hold.");
        MESSAGES.put("adjusted", "Commission adjusted. The change is in the audit log.");
        MESSAGES.put("paid", "Payout recorded.");
        MESSAGES.put("nothing", "Nothing to do.");
        MESSAGES.put("requested", "Payout requested.");
        MESSAGES.put("settings", "Settings saved.");
        MESSAGES.put("cancelled", "Cancelled.");
        MESSAGES.put("imported", "Import finished.");
        MESSAGES.put("duplicate", "A commission for that platform order already exists.");
        MESSAGES.put("reason", "A reason of at least 5 characters is required.");
        MESSAGES.put("draft", "Draft payout created.");
        MESSAGES.put("confirmed", "Payout confirmed. The commissions are now paid and locked.");
        MESSAGES.put("removed", "Commission removed from the draft payout.");
        MESSAGES.put("stale", "The commissions changed while you were working. Nothing was saved, please try again.");
    }

    protected static User user(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
    }

    protected static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    protected static int intParam(HttpServletRequest request, String name, int fallback) {
        try {
            return Integer.parseInt(trim(request.getParameter(name)));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    protected static String oneOf(String value, String... allowed) {
        for (String a : allowed) {
            if (a.equals(value)) {
                return a;
            }
        }
        return "";
    }

    protected static void redirect(HttpServletRequest request, HttpServletResponse response, String path, String msg) throws IOException {
        response.sendRedirect(request.getContextPath() + path + (path.contains("?") ? "&" : "?") + "msg=" + URLEncoder.encode(msg, "UTF-8"));
    }

    protected static void csv(HttpServletResponse response, String name, String[] header, java.util.List<String[]> rows) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + name + ".csv\"");
        StringBuilder sb = new StringBuilder("﻿");
        appendRow(sb, header);
        for (String[] r : rows) {
            appendRow(sb, r);
        }
        response.getWriter().write(sb.toString());
    }

    private static void appendRow(StringBuilder sb, String[] cells) {
        for (int i = 0; i < cells.length; i++) {
            String c = cells[i] == null ? "" : cells[i];
            if (!c.isEmpty() && "=+-@".indexOf(c.charAt(0)) >= 0) {
                c = "'" + c;
            }
            sb.append(i == 0 ? "" : ",").append('"').append(c.replace("\"", "\"\"")).append('"');
        }
        sb.append("\r\n");
    }
}
