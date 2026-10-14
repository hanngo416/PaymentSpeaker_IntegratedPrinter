package controller.transaction;

import controller.auth.LoginServlet;
import dao.RefundDAO;
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
import model.enums.Role;
import service.AuditService;

/**
 * Money that reached the account but is not kept for an order. The cashier sends it back by bank transfer, then marks it done here.
 */
@WebServlet(name = "RefundServlet", urlPatterns = {"/refunds"})
public class RefundServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/transaction/refunds.jsp";
    private static final List<String> STATUSES = Arrays.asList("PENDING", "DONE");
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("done", "Refund marked as done.");
        MESSAGES.put("stale", "This refund was already handled, or it is not yours.");
        MESSAGES.put("note", "Enter the bank reference of the refund transfer.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final RefundDAO refundDAO = new RefundDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String status = request.getParameter("status");
        String filter = status == null ? "PENDING" : STATUSES.contains(status) ? status : null;
        try {
            request.setAttribute("refunds", refundDAO.search(filter, cashierScope(actor)));
        } catch (SQLException e) {
            throw new ServletException("Cannot load refunds", e);
        }
        request.setAttribute("statuses", STATUSES);
        request.setAttribute("status", filter);
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        Integer id = parseInt(request.getParameter("id"));
        String note = request.getParameter("note");
        note = note == null ? "" : note.trim();
        if (note.length() > 500) {
            note = note.substring(0, 500);
        }

        String msg;
        if (id == null) {
            msg = "bad";
        } else if (note.isEmpty()) {
            msg = "note";
        } else {
            try {
                boolean done = refundDAO.markDone(id, actor.getUserId(), note, cashierScope(actor));
                if (done) {
                    AuditService.log(actor, "REFUND_DONE", "REFUND", id, "note", note);
                }
                msg = done ? "done" : "stale";
            } catch (SQLException e) {
                throw new ServletException("Cannot update the refund", e);
            }
        }
        response.sendRedirect(request.getContextPath() + "/refunds?status=PENDING&msg=" + msg);
    }

    // A cashier only sees and handles refunds of the orders they created
    private static Integer cashierScope(User actor) {
        return actor.getRole() == Role.CASHIER ? actor.getUserId() : null;
    }

    private static Integer parseInt(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
