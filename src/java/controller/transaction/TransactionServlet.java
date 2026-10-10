package controller.transaction;

import dao.DeviceDAO;
import dao.TransactionDAO;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.Date;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Device;
import model.Page;
import model.TxFilter;
import model.enums.TxLabel;
import model.view.ChartBar;
import model.view.DayTotal;
import model.view.TxRow;

@WebServlet(name = "TransactionServlet", urlPatterns = {"/transactions"})
public class TransactionServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/transaction/transactions.jsp";
    private static final int CHART_WIDTH = 700;
    private static final int CHART_HEIGHT = 240;
    private static final int CHART_LEFT = 70;
    private static final long DEFAULT_RANGE_DAYS = 30;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();

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
        f.setDeviceId(parseInt(request.getParameter("device")));
        f.setLabel(oneOfLabels(request.getParameter("label")));
        Integer page = parseInt(request.getParameter("page"));
        f.setPage(page == null || page < 1 ? 1 : page);

        try {
            Page<TxRow> result = transactionDAO.search(f);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                f.setPage(result.getTotalPages());
                result = transactionDAO.search(f);
            }
            List<DayTotal> days = transactionDAO.dailyTotals(f);
            List<Device> devices = deviceDAO.findAll();

            request.setAttribute("result", result);
            request.setAttribute("f", f);
            request.setAttribute("devices", devices);
            request.setAttribute("labels", TxLabel.values());
            request.setAttribute("fromText", from == null ? "" : from.toString());
            request.setAttribute("toText", to == null ? "" : to.toString());
            request.setAttribute("filterQuery", filterQuery(f, from, to));
            request.setAttribute("bars", buildBars(days));
            request.setAttribute("chartWidth", CHART_WIDTH);
            request.setAttribute("chartHeight", CHART_HEIGHT);
            request.setAttribute("chartLeft", CHART_LEFT);
            request.setAttribute("chartMax", formatMoney(maxTotal(days)));
        } catch (SQLException e) {
            throw new ServletException("Cannot load transactions", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static BigDecimal maxTotal(List<DayTotal> days) {
        BigDecimal max = BigDecimal.ZERO;
        for (DayTotal d : days) {
            BigDecimal total = d.getLegitTotal().add(d.getFlaggedTotal());
            if (total.compareTo(max) > 0) {
                max = total;
            }
        }
        return max;
    }

    private static List<ChartBar> buildBars(List<DayTotal> days) {
        List<ChartBar> bars = new ArrayList<>();
        if (days.isEmpty()) {
            return bars;
        }
        BigDecimal max = maxTotal(days);
        if (max.signum() == 0) {
            max = BigDecimal.ONE;
        }
        int slot = (CHART_WIDTH - CHART_LEFT) / days.size();
        int barWidth = Math.max(4, slot * 7 / 10);
        int labelEvery = days.size() > 15 ? 3 : 1;
        SimpleDateFormat dayFormat = new SimpleDateFormat("MM-dd");
        for (int i = 0; i < days.size(); i++) {
            DayTotal d = days.get(i);
            int legitH = scale(d.getLegitTotal(), max);
            int flaggedH = scale(d.getFlaggedTotal(), max);
            ChartBar b = new ChartBar();
            b.setX(CHART_LEFT + i * slot + (slot - barWidth) / 2);
            b.setWidth(barWidth);
            b.setLegitHeight(legitH);
            b.setLegitY(CHART_HEIGHT - legitH);
            b.setFlaggedHeight(flaggedH);
            b.setFlaggedY(CHART_HEIGHT - legitH - flaggedH);
            b.setLabel(i % labelEvery == 0 ? dayFormat.format(d.getDay()) : "");
            b.setTip(d.getDay() + ": " + d.getCount() + " transaction(s), valid "
                    + formatMoney(d.getLegitTotal()) + " VND, flagged "
                    + formatMoney(d.getFlaggedTotal()) + " VND");
            bars.add(b);
        }
        return bars;
    }

    private static int scale(BigDecimal value, BigDecimal max) {
        return value.multiply(BigDecimal.valueOf(CHART_HEIGHT)).divide(max, 0, BigDecimal.ROUND_HALF_UP).intValue();
    }

    private static String formatMoney(BigDecimal value) {
        return new DecimalFormat("#,##0").format(value);
    }

    private static String filterQuery(TxFilter f, Date from, Date to) {
        StringBuilder q = new StringBuilder();
        append(q, "device", f.getDeviceId() == null ? null : String.valueOf(f.getDeviceId()));
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
        for (TxLabel l : TxLabel.values()) {
            if (l.name().equals(value)) {
                return value;
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
