/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller.tax;

import controller.affiliate.AffiliateBase;
import dao.SettingDAO;
import dao.TaxToolsDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.AuditService;
import service.TaxToolsService;
import static util.Params.parseDate;

/**
 *
 * @author PC
 */
@WebServlet(name = "TaxViewServlet", urlPatterns = {"/tax/reports", "/tax/calendar", "/tax/assistant"})
public class TaxViewServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/tax/";
    private static final String[] REMINDERS = {"d7", "d3", "d1", "mail", "app"};

    private final TaxToolsDAO dao = new TaxToolsDAO();
    private final SettingDAO settingDAO = new SettingDAO();
    private final TaxToolsService tools = new TaxToolsService();

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            switch (request.getServletPath()) {
                case "/tax/reports":
                    showReports(request, response);
                    break;
                case "/tax/calendar":
                    showCalendar(request, response);
                    break;
                default:
                    request.getRequestDispatcher(VIEW + "assistant.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load the tax page", e);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/tax/calendar".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        try {
            StringBuilder state = new StringBuilder();
            for (String key : REMINDERS) {
                boolean on = request.getParameter(key) != null;
                settingDAO.set("taxrem_" + key, on ? "1" : "0");
                state.append(key).append('=').append(on ? 1 : 0).append(' ');
            }
            AuditService.log(user(request), "TAX_REMINDER_SET", "TAX", "calendar", state.toString().trim());
            response.sendRedirect(request.getContextPath() + "/tax/calendar?view=" + (("list".equals(request.getParameter("view"))) ? "list" : "grid") + "&saved=1");
        } catch (SQLException e) {
            throw new ServletException("Cannot save the reminder settings", e);
        }
    }

    private void showReports(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        LocalDate today = LocalDate.now();
        String range = oneOf(trim(request.getParameter("range")), "ytd", "m0", "m1", "q", "custom");
        if (range.isEmpty()) {
            range = "ytd";
        }
        LocalDate from = today.withDayOfYear(1);
        LocalDate to = today;
        String error = null;
        switch (range) {
            case "m0":
                from = today.withDayOfMonth(1);
                break;
            case "m1":
                from = today.minusMonths(1).withDayOfMonth(1);
                to = from.withDayOfMonth(from.lengthOfMonth());
                break;
            case "q":
                from = LocalDate.of(today.getYear(), ((today.getMonthValue() - 1) / 3) * 3 + 1, 1);
                break;
            case "custom":
                Date f = parseDate(request.getParameter("from"));
                Date t = parseDate(request.getParameter("to"));
                if (f == null || t == null) {
                    error = "Hãy chọn đủ ngày bắt đầu và ngày kết thúc.";
                } else if (f.after(t)) {
                    error = "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.";
                } else if (f.toLocalDate().getYear() < 2000 || t.toLocalDate().isAfter(today)) {
                    error = "Kỳ báo cáo phải từ năm 2000 đến hôm nay.";
                } else if (t.toLocalDate().toEpochDay() - f.toLocalDate().toEpochDay() > 1830) {
                    error = "Kỳ báo cáo tối đa 5 năm.";
                } else {
                    from = f.toLocalDate();
                    to = t.toLocalDate();
                }
                break;
            default:
        }
        Report report = build(from, to);
        if ("csv".equals(request.getParameter("export")) && error == null) {
            List<String[]> rows = new ArrayList<>();
            for (Map<String, Object> m : report.months) {
                rows.add(new String[]{String.valueOf(m.get("ym")), plain(m.get("store")), plain(m.get("shopee")), plain(m.get("tiktok")),
                    plain(m.get("discounts")), plain(m.get("removed")), plain(m.get("taxable"))});
            }
            rows.add(new String[]{"TOTAL", plain(report.store), plain(report.shopee), plain(report.tiktok), plain(report.discounts), plain(report.removed), plain(report.taxable)});
            csv(response, "doanh-thu-" + from + "-" + to, new String[]{"Month", "Store net", "Shopee", "TikTok", "Discounts given", "Cancelled or returned", "Taxable revenue"}, rows);
            return;
        }
        List<String> warnings = new ArrayList<>();
        int completion = tools.completion(tools.profile());
        if (completion < 100) {
            warnings.add("Hồ sơ thuế mới hoàn thành " + completion + "%.");
        }
        if (report.shopee.signum() + report.tiktok.signum() == 0) {
            warnings.add("Chưa có đơn từ sàn Shopee hoặc TikTok trong kỳ này. Nếu bạn có bán online, hãy nhập file đơn ở mục Sales book.");
        }
        request.setAttribute("report", report);
        request.setAttribute("range", range);
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.setAttribute("error", error);
        request.setAttribute("warnings", warnings);
        request.setAttribute("maxMonth", report.months.stream().map(m -> (BigDecimal) m.get("taxable")).max(BigDecimal::compareTo).orElse(BigDecimal.ONE).max(BigDecimal.ONE));
        request.getRequestDispatcher(VIEW + "reports.jsp").forward(request, response);
    }

    private Report build(LocalDate from, LocalDate to) throws SQLException {
        Map<String, Map<String, Object>> byMonth = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ym", m.toString());
            for (String k : new String[]{"store", "shopee", "tiktok", "discounts", "removed", "taxable"}) {
                row.put(k, BigDecimal.ZERO);
            }
            byMonth.put(m.toString(), row);
        }
        for (Map<String, Object> r : dao.storeMonthly(Date.valueOf(from), Date.valueOf(to))) {
            Map<String, Object> row = byMonth.get(String.valueOf(r.get("ym")));
            if (row != null) {
                add(row, "store", r.get("net"));
                add(row, "discounts", r.get("discounts"));
                add(row, "removed", r.get("cancelled"));
            }
        }
        for (Map<String, Object> r : dao.channelMonthly(Date.valueOf(from), Date.valueOf(to))) {
            Map<String, Object> row = byMonth.get(String.valueOf(r.get("ym")));
            if (row != null) {
                add(row, "SHOPEE".equals(r.get("channel")) ? "shopee" : "tiktok", r.get("net"));
                add(row, "removed", r.get("removed"));
            }
        }
        Report report = new Report();
        for (Map<String, Object> row : byMonth.values()) {
            BigDecimal taxable = ((BigDecimal) row.get("store")).add((BigDecimal) row.get("shopee")).add((BigDecimal) row.get("tiktok"));
            row.put("taxable", taxable);
            report.store = report.store.add((BigDecimal) row.get("store"));
            report.shopee = report.shopee.add((BigDecimal) row.get("shopee"));
            report.tiktok = report.tiktok.add((BigDecimal) row.get("tiktok"));
            report.discounts = report.discounts.add((BigDecimal) row.get("discounts"));
            report.removed = report.removed.add((BigDecimal) row.get("removed"));
            report.taxable = report.taxable.add(taxable);
            report.months.add(row);
        }
        report.gross = report.taxable.add(report.discounts);
        return report;
    }

    private static void add(Map<String, Object> row, String key, Object value) {
        if (value instanceof BigDecimal) {
            row.put(key, ((BigDecimal) row.get(key)).add((BigDecimal) value));
        }
    }

    private static String plain(Object value) {
        return value == null ? "" : ((BigDecimal) value).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    public static class Report {

        private final List<Map<String, Object>> months = new ArrayList<>();
        private BigDecimal store = BigDecimal.ZERO;
        private BigDecimal shopee = BigDecimal.ZERO;
        private BigDecimal tiktok = BigDecimal.ZERO;
        private BigDecimal discounts = BigDecimal.ZERO;
        private BigDecimal removed = BigDecimal.ZERO;
        private BigDecimal taxable = BigDecimal.ZERO;
        private BigDecimal gross = BigDecimal.ZERO;

        public List<Map<String, Object>> getMonths() {
            return months;
        }

        public BigDecimal getStore() {
            return store;
        }

        public BigDecimal getShopee() {
            return shopee;
        }

        public BigDecimal getTiktok() {
            return tiktok;
        }

        public BigDecimal getDiscounts() {
            return discounts;
        }

        public BigDecimal getRemoved() {
            return removed;
        }

        public BigDecimal getTaxable() {
            return taxable;
        }

        public BigDecimal getGross() {
            return gross;
        }
    }

    private void showCalendar(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        YearMonth month = YearMonth.now();
        String raw = trim(request.getParameter("m"));
        if (raw.matches("\\d{4}-\\d{2}")) {
            try {
                YearMonth parsed = YearMonth.parse(raw);
                if (parsed.getYear() >= 2000 && parsed.getYear() <= 2100) {
                    month = parsed;
                }
            } catch (java.time.format.DateTimeParseException e) {
                month = YearMonth.now();
            }
        }
        List<Map<String, Object>> all = dao.obligations();
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> cells = new ArrayList<>();
        int blanks = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 0; i < blanks; i++) {
            cells.add(null);
        }
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            LocalDate date = month.atDay(d);
            Map<String, Object> cell = new LinkedHashMap<>();
            List<Map<String, Object>> items = new ArrayList<>();
            for (Map<String, Object> o : all) {
                if (date.equals(((Date) o.get("due_date")).toLocalDate())) {
                    items.add(o);
                }
            }
            cell.put("day", d);
            cell.put("today", date.equals(today));
            cell.put("items", items);
            cells.add(cell);
        }
        List<Map<String, Object>> upcoming = new ArrayList<>();
        for (Map<String, Object> o : all) {
            String status = String.valueOf(o.get("status"));
            if (!"PAID".equals(status) && !"NOT_APPLICABLE".equals(status) && upcoming.size() < 5) {
                upcoming.add(o);
            }
        }
        Map<String, Boolean> reminders = new LinkedHashMap<>();
        for (String key : REMINDERS) {
            String fallback = "d7".equals(key) || "d3".equals(key) || "app".equals(key) ? "1" : "0";
            reminders.put(key, "1".equals(settingDAO.get("taxrem_" + key, fallback)));
        }
        request.setAttribute("month", month.toString());
        request.setAttribute("monthLabel", month.getMonthValue() + "/" + month.getYear());
        request.setAttribute("prevMonth", month.minusMonths(1).toString());
        request.setAttribute("nextMonth", month.plusMonths(1).toString());
        request.setAttribute("cells", cells);
        request.setAttribute("tasks", all);
        request.setAttribute("upcoming", upcoming);
        request.setAttribute("reminders", reminders);
        request.setAttribute("view", "list".equals(request.getParameter("view")) ? "list" : "grid");
        request.setAttribute("savedMsg", request.getParameter("saved") != null);
        request.setAttribute("canManage", canManage(request));
        request.getRequestDispatcher(VIEW + "calendar.jsp").forward(request, response);
    }

    static boolean canManage(HttpServletRequest request) {
        return filter.RolePermissions.has(user(request).getRole(), model.enums.Permission.TAX_MANAGE);
    }
}
