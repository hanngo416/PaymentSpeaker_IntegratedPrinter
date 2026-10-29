package controller.admin;

import dao.CatalogDAO;
import dao.StockDAO;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.view.StockRow;
import static util.Params.parseDate;

@WebServlet(name = "StockServlet", urlPatterns = {"/admin/stock-history"})
public class StockServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/stock-history.jsp";
    private static final int PAGE_SIZE = 10;

    private final StockDAO stockDAO = new StockDAO();
    private final CatalogDAO catalogDAO = new CatalogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
            page = 1;
        }
        String rawProduct = request.getParameter("productId");
        Integer productId = rawProduct != null && rawProduct.matches("\\d{1,9}") ? Integer.valueOf(rawProduct) : null;
        String rawReason = request.getParameter("reason");
        String reason = StockDAO.SALE.equals(rawReason) || StockDAO.RESTOCK.equals(rawReason)
                || StockDAO.ADJUST.equals(rawReason) || StockDAO.INITIAL.equals(rawReason) ? rawReason : null;
        String from = request.getParameter("from");
        String to = request.getParameter("to");
        java.sql.Date fromDate = parseDate(from);
        java.sql.Date toDate = parseDate(to);
        try {
            Page<StockRow> result = stockDAO.search(productId, reason, fromDate, toDate, page, PAGE_SIZE);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                result = stockDAO.search(productId, reason, fromDate, toDate, result.getTotalPages(), PAGE_SIZE);
            }
            request.setAttribute("result", result);
            request.setAttribute("products", catalogDAO.findActiveProducts());
        } catch (SQLException e) {
            throw new ServletException("Cannot load the stock history", e);
        }
        request.setAttribute("productId", productId);
        request.setAttribute("reason", reason);
        request.setAttribute("from", fromDate == null ? null : from);
        request.setAttribute("to", toDate == null ? null : to);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
