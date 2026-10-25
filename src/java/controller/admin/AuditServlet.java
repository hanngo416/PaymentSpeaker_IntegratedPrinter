package controller.admin;

import dao.AuditDAO;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static util.Params.parseDate;

@WebServlet(name = "AuditServlet", urlPatterns = {"/admin/audit"})
public class AuditServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/audit.jsp";
    private static final int PAGE_SIZE = 10;

    private final AuditDAO auditDAO = new AuditDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
        }
        try {
            String q = trimToNull(request.getParameter("q"));
            java.util.List<String> actions = new java.util.ArrayList<>();
            String[] picked = request.getParameterValues("action");
            for (int i = 0; picked != null && i < picked.length && i < 40; i++) {
                String a = trimToNull(picked[i]);
                if (a != null) {
                    actions.add(a);
                }
            }
            String from = trimToNull(request.getParameter("from"));
            String to = trimToNull(request.getParameter("to"));
            java.sql.Date fromDate = parseDate(from);
            java.sql.Date toDate = parseDate(to);
            model.Page<model.view.AuditRow> result = auditDAO.search(page, PAGE_SIZE, q, actions, fromDate, toDate);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                result = auditDAO.search(result.getTotalPages(), PAGE_SIZE, q, actions, fromDate, toDate);
            }
            request.setAttribute("result", result);
            request.setAttribute("actions", auditDAO.findActions());
            request.setAttribute("q", q);
            request.setAttribute("pickedActions", new java.util.HashSet<>(actions));
            StringBuilder query = new StringBuilder();
            for (String a : actions) {
                query.append("&action=").append(java.net.URLEncoder.encode(a, "UTF-8"));
            }
            request.setAttribute("actionQuery", query.toString());
            request.setAttribute("from", fromDate == null ? null : from);
            request.setAttribute("to", toDate == null ? null : to);
        } catch (SQLException e) {
            throw new ServletException("Cannot load the audit trail", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : (t.length() > 100 ? t.substring(0, 100) : t);
    }
}
