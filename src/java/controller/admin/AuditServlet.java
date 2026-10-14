package controller.admin;

import dao.AuditDAO;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.AuditService;

/**
 * Audit trail: /admin/audit lists the records, newest first. Adding verify=1 also walks the whole hash chain.
 */
@WebServlet(name = "AuditServlet", urlPatterns = {"/admin/audit"})
public class AuditServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/audit.jsp";
    private static final int PAGE_SIZE = 50;

    private final AuditDAO auditDAO = new AuditDAO();
    private final AuditService auditService = new AuditService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
            // no or bad page number: first page
        }
        try {
            model.Page<model.view.AuditRow> result = auditDAO.search(page, PAGE_SIZE);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                result = auditDAO.search(result.getTotalPages(), PAGE_SIZE);
            }
            request.setAttribute("result", result);
            if ("1".equals(request.getParameter("verify"))) {
                request.setAttribute("check", auditService.verify());
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load the audit trail", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
