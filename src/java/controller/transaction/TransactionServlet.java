package controller.transaction;

import dao.TransactionDAO;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.TxFilter;
import model.enums.TxLabel;
import model.view.DayTotal;
import model.view.TxRow;
import util.TxChart;

@WebServlet(name = "TransactionServlet", urlPatterns = {"/transactions"})
public class TransactionServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/transaction/transactions.jsp";
    private static final long DEFAULT_RANGE_DAYS = 30;

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long dayMillis = 24L * 60 * 60 * 1000;
        Date today = new Date(System.currentTimeMillis());
        Date from = parseDate(request.getParameter("from"));
        Date to = parseDate(request.getParameter("to"));
        if (from == null && to == null && request.getParameter("from") == null) {
            to = today;
            from = new Date(today.getTime() - DEFAULT_RANGE_DAYS * dayMillis);
        }

        TxFilter f = new TxFilter();
        f.setFrom(from);
        f.setTo(to);
        String statusParam = request.getParameter("status");
        if (statusParam == null || statusParam.trim().isEmpty()) {
            statusParam = request.getParameter("label");
        }
        f.setLabel(oneOfLabels(statusParam));
        Integer page = parseInt(request.getParameter("page"));
        f.setPage(page == null || page < 1 ? 1 : page);

        try {
            Page<TxRow> result = transactionDAO.search(f);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                f.setPage(result.getTotalPages());
                result = transactionDAO.search(f);
            }
            List<DayTotal> days = transactionDAO.dailyTotals(f);

            request.setAttribute("result", result);
            request.setAttribute("f", f);
            request.setAttribute("labels", new String[]{"COMPLETED", "CANCELLED"});
            request.setAttribute("fromText", from == null ? "" : from.toString());
            request.setAttribute("toText", to == null ? "" : to.toString());
            request.setAttribute("filterQuery", filterQuery(f, from, to));
            BigDecimal sumValid = BigDecimal.ZERO;
            BigDecimal sumFlagged = BigDecimal.ZERO;
            for (DayTotal d : days) {
                sumValid = sumValid.add(d.getLegitTotal());
                sumFlagged = sumFlagged.add(d.getFlaggedTotal());
            }
            request.setAttribute("sumValid", TxChart.money(sumValid));
            request.setAttribute("sumFlagged", TxChart.money(sumFlagged));
            BigDecimal ceiling = TxChart.ceiling(days);
            request.setAttribute("bars", TxChart.bars(days, ceiling));
            request.setAttribute("ticks", TxChart.ticks(ceiling));
            request.setAttribute("chartWidth", TxChart.WIDTH);
            request.setAttribute("chartHeight", TxChart.HEIGHT);
            request.setAttribute("chartLeft", TxChart.LEFT);
            request.setAttribute("chartTop", TxChart.TOP);
            request.setAttribute("chartMax", TxChart.money(ceiling));
        } catch (SQLException e) {
            throw new ServletException("Cannot load transactions", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String filterQuery(TxFilter f, Date from, Date to) {
        StringBuilder q = new StringBuilder();
        append(q, "label", f.getLabel());
        q.append("from=").append(from == null ? "" : from.toString()).append('&');
        append(q, "to", to == null ? null : to.toString());
        return q.toString();
    }

    private static void append(StringBuilder q, String name, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            q.append(name).append('=').append(URLEncoder.encode(value, "UTF-8")).append('&');
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String oneOfLabels(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String v = value.trim().toUpperCase();
        if ("COMPLETED".equals(v) || "CANCELLED".equals(v)) {
            return v;
        }
        for (TxLabel l : TxLabel.values()) {
            if (l.name().equals(v)) {
                return v;
            }
        }
        return null;
    }

    private static Date parseDate(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Date.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Integer parseInt(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
