package controller.affiliate;

import dao.AffiliateReportDAO;
import dao.CommissionDAO;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;

@WebServlet(name = "ReferralServlet", urlPatterns = {"/admin/referrals"})
public class ReferralServlet extends AffiliateBase {

    private final CommissionDAO commissionDAO = new CommissionDAO();
    private final AffiliateReportDAO reportDAO = new AffiliateReportDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String q = trim(request.getParameter("q"));
            String status = oneOf(trim(request.getParameter("status")), CommissionDAO.ALL_STATUSES);
            int days = Math.max(0, Math.min(365, intParam(request, "days", 0)));
            request.setAttribute("result", Page.of(commissionDAO.search(q, status, null, days), intParam(request, "page", 1), 10));
            request.setAttribute("flags", reportDAO.flags());
            request.setAttribute("q", q);
            request.setAttribute("statusFilter", status);
            request.setAttribute("daysFilter", days);
            request.setAttribute("statuses", CommissionDAO.ALL_STATUSES);
            int open = intParam(request, "id", 0);
            if (open > 0) {
                request.setAttribute("detail", commissionDAO.find(open));
            }
            request.getRequestDispatcher("/WEB-INF/views/admin/referrals.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load referrals", e);
        }
    }
}
