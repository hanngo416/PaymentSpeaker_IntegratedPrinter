package controller.tax;

import dao.SettingDAO;
import dao.TaxToolsDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.TaxService;
import service.TaxToolsService;

@WebServlet(name = "TaxServlet", urlPatterns = {"/tax"})
public class TaxServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/tax/tax.jsp";

    private final TaxService taxService = new TaxService();
    private final TaxToolsService taxToolsService = new TaxToolsService();
    private final TaxToolsDAO dao = new TaxToolsDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int now = LocalDate.now().getYear();
        int year = now;
        String raw = request.getParameter("year");
        if (raw != null && raw.matches("20\\d{2}")) {
            year = Integer.parseInt(raw);
        }
        try {
            request.setAttribute("tax", taxService.view(year));
            Map<String, String> profile = taxToolsService.profile();
            request.setAttribute("profile", profile);
            request.setAttribute("completion", taxToolsService.completion(profile));

            LocalDate today = LocalDate.now();
            Date startOfMonth = Date.valueOf(today.withDayOfMonth(1));
            Date endOfToday = Date.valueOf(today);
            BigDecimal monthRevenue = BigDecimal.ZERO;
            try {
                monthRevenue = monthRevenue.add(taxToolsService.revenue(startOfMonth, endOfToday));
                List<Map<String, Object>> chRows = dao.channelMonthly(startOfMonth, endOfToday);
                for (Map<String, Object> r : chRows) {
                    Object net = r.get("net");
                    if (net instanceof BigDecimal) {
                        monthRevenue = monthRevenue.add((BigDecimal) net);
                    } else if (net != null) {
                        monthRevenue = monthRevenue.add(new BigDecimal(net.toString()));
                    }
                }
            } catch (SQLException ignored) {
            }
            request.setAttribute("monthRevenue", monthRevenue);

            List<Map<String, Object>> obligations = new ArrayList<>();
            try {
                obligations = dao.obligations();
            } catch (SQLException ignored) {
            }
            request.setAttribute("obligations", obligations);

            BigDecimal affComTotal = BigDecimal.ZERO;
            try {
                List<Map<String, Object>> affRows = dao.query(
                        "SELECT COALESCE(SUM(amount), 0) AS total FROM Commissions WHERE status NOT IN ('REJECTED', 'REVERSED') AND YEAR(created_at) = ?",
                        year);
                if (!affRows.isEmpty() && affRows.get(0).get("total") != null) {
                    affComTotal = new BigDecimal(affRows.get(0).get("total").toString());
                }
            } catch (SQLException ignored) {
            }
            request.setAttribute("affiliateCommissionTotal", affComTotal);
            request.setAttribute("affiliateWithholdPct", settingDAO.get("aff_withhold_pct", "10"));
            request.setAttribute("affiliateWithholdFrom", settingDAO.get("aff_withhold_from", "60000"));
        } catch (SQLException e) {
            throw new ServletException("Cannot build the tax estimate", e);
        }
        request.setAttribute("year", year);
        request.setAttribute("thisYear", now);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
