package controller.transaction;

import controller.auth.LoginServlet;
import dao.TransactionReviewDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.User;
import model.view.ReviewRow;
import service.AuditService;
import static util.Params.parseDate;

@WebServlet(name = "TransactionReviewServlet", urlPatterns = {"/transactions/review"})
public class TransactionReviewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/transaction/review.jsp";
    private static final int PAGE_SIZE = 10;
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("linked", "The transfer is linked and the order is completed.");
        MESSAGES.put("closed", "The transfer is closed.");
        MESSAGES.put("bad", "Check the order, the category and the note (5 to 200 characters), then try again.");
        MESSAGES.put("notfound", "The transfer or the order was not found.");
        MESSAGES.put("resolved", "This transfer was already handled.");
        MESSAGES.put("notpending", "Only an order that is still waiting for payment can be linked.");
        MESSAGES.put("amountlow", "The transfer is smaller than the amount the order needs. Close it with Refund needed instead.");
        MESSAGES.put("orderlinked", "Another transfer is already linked to this order.");
    }

    private final TransactionReviewDAO reviewDAO = new TransactionReviewDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
            page = 1;
        }
        String rawRule = request.getParameter("rule");
        String rule = rawRule != null && rawRule.matches("[A-Z_]{1,50}") ? rawRule : null;
        String from = request.getParameter("from");
        String to = request.getParameter("to");
        java.sql.Date fromDate = parseDate(from);
        java.sql.Date toDate = parseDate(to);
        try {
            Page<ReviewRow> result = reviewDAO.search(rule, fromDate, toDate, page, PAGE_SIZE);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                result = reviewDAO.search(rule, fromDate, toDate, result.getTotalPages(), PAGE_SIZE);
            }
            request.setAttribute("result", result);
            request.setAttribute("rules", reviewDAO.findRuleCodes());
            request.setAttribute("pendingOrders", reviewDAO.findPendingOrders());
        } catch (SQLException e) {
            throw new ServletException("Cannot load the transfers to review", e);
        }
        request.setAttribute("categories", TransactionReviewDAO.CATEGORIES);
        request.setAttribute("rule", rule);
        request.setAttribute("from", fromDate == null ? null : from);
        request.setAttribute("to", toDate == null ? null : to);
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        String rawTrans = trim(request.getParameter("transId"));
        String note = trim(request.getParameter("note"));
        String msg = "bad";
        if (rawTrans.matches("\\d{1,9}") && note.length() >= 5 && note.length() <= 200) {
            int transId = Integer.parseInt(rawTrans);
            try {
                if ("link".equals(action)) {
                    String rawOrder = trim(request.getParameter("orderId"));
                    if (rawOrder.matches("\\d{1,9}")) {
                        int orderId = Integer.parseInt(rawOrder);
                        String result = reviewDAO.link(transId, orderId, actor.getUserId(), note);
                        msg = message(result, "linked");
                        if (TransactionReviewDAO.OK.equals(result)) {
                            AuditService.log(actor, "TRANSACTION_LINK", "TRANSACTION", String.valueOf(transId),
                                    "order_id", String.valueOf(orderId), "note", note);
                        }
                    }
                } else if ("close".equals(action)) {
                    String category = trim(request.getParameter("category"));
                    if (TransactionReviewDAO.CATEGORIES.contains(category)) {
                        String result = reviewDAO.close(transId, category, actor.getUserId(), note);
                        msg = message(result, "closed");
                        if (TransactionReviewDAO.OK.equals(result)) {
                            AuditService.log(actor, "TRANSACTION_CLOSE", "TRANSACTION", String.valueOf(transId),
                                    "category", category, "note", note);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new ServletException("Cannot save the review", e);
            }
        }
        response.sendRedirect(request.getContextPath() + "/transactions/review?msg=" + msg);
    }

    private static String message(String result, String okKey) {
        switch (result) {
            case TransactionReviewDAO.OK:
                return okKey;
            case TransactionReviewDAO.NOT_FOUND:
                return "notfound";
            case TransactionReviewDAO.ALREADY_RESOLVED:
                return "resolved";
            case TransactionReviewDAO.ORDER_NOT_PENDING:
                return "notpending";
            case TransactionReviewDAO.AMOUNT_TOO_LOW:
                return "amountlow";
            case TransactionReviewDAO.ORDER_ALREADY_LINKED:
                return "orderlinked";
            default:
                return "bad";
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
