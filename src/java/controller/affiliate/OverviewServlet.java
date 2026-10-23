package controller.affiliate;

import dao.AffiliateDAO;
import dao.AffiliateReportDAO;
import dao.CommissionDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Affiliate;
import model.Commission;

@WebServlet(name = "OverviewServlet", urlPatterns = {"/admin/affiliate-overview"})
public class OverviewServlet extends AffiliateBase {

    private final AffiliateReportDAO reportDAO = new AffiliateReportDAO();
    private final CommissionDAO commissionDAO = new CommissionDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int days = intParam(request, "days", 30);
        if (days != 7 && days != 30 && days != 90 && days != 365) {
            days = 30;
        }
        try {
            if ("1".equals(request.getParameter("export"))) {
                List<String[]> rows = new ArrayList<>();
                for (Affiliate a : affiliateDAO.search("", "")) {
                    rows.add(new String[]{a.getReferralCode(), a.getName(), a.getStatus(), String.valueOf(a.getOrderCount()),
                        a.getRevenue().toPlainString(), a.getCommissionTotal().toPlainString()});
                }
                csv(response, "affiliate-performance", new String[]{"Code", "Name", "Status", "Orders", "Revenue", "Commission"}, rows);
                return;
            }
            List<Object[]> daily = reportDAO.daily(days);
            BigDecimal max = BigDecimal.ONE;
            for (Object[] d : daily) {
                max = max.max((BigDecimal) d[2]);
            }
            List<Commission> pending = new ArrayList<>();
            for (Commission c : commissionDAO.search(null, CommissionDAO.PENDING, null, 0)) {
                if (pending.size() < 5) {
                    pending.add(c);
                }
            }
            List<Commission> recent = commissionDAO.search(null, null, null, 0);
            request.setAttribute("kpi", reportDAO.kpis(days));
            request.setAttribute("daily", daily);
            request.setAttribute("dailyMax", max);
            request.setAttribute("top", reportDAO.topAffiliates(days, 5));
            request.setAttribute("byCampaign", reportDAO.byCampaign(days));
            request.setAttribute("products", reportDAO.topProducts(days, 6));
            request.setAttribute("flags", reportDAO.flags());
            request.setAttribute("pending", pending);
            request.setAttribute("recent", recent.subList(0, Math.min(6, recent.size())));
            request.setAttribute("days", days);
            request.getRequestDispatcher("/WEB-INF/views/admin/affiliate-overview.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load the affiliate overview", e);
        }
    }
}
