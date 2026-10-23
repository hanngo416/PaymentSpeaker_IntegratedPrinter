package controller.affiliate;

import dao.AffiliateDAO;
import dao.PayoutDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Affiliate;
import model.Payout;
import model.User;
import service.AffiliateService;
import service.AuditService;

@WebServlet(name = "PayoutServlet", urlPatterns = {"/admin/payouts"})
public class PayoutServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/payouts.jsp";

    private final PayoutDAO payoutDAO = new PayoutDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();
    private final AffiliateService affiliateService = new AffiliateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            affiliateService.releaseMatured();
            String q = trim(request.getParameter("q"));
            String status = oneOf(trim(request.getParameter("status")), "REQUESTED", "PAID");
            List<Payout> all = payoutDAO.search(q, status, null);
            if ("1".equals(request.getParameter("export"))) {
                List<String[]> rows = new ArrayList<>();
                for (Payout p : all) {
                    rows.add(new String[]{String.valueOf(p.getPayoutId()), p.getAffiliateName(), p.getBankInfo(), p.getGross().toPlainString(), p.getDeduction().toPlainString(),
                        p.getNet().toPlainString(), String.valueOf(p.getPaidAt() == null ? "" : p.getPaidAt()), p.getReference(), p.getStatus()});
                }
                csv(response, "payouts", new String[]{"ID", "Affiliate", "Bank", "Gross", "Deductions", "Net", "Paid at", "Reference", "Status"}, rows);
                return;
            }
            long min = affiliateService.payoutMin();
            Map<Integer, BigDecimal> available = payoutDAO.availableByAffiliate();
            List<Object[]> eligible = new ArrayList<>();
            BigDecimal totalAvailable = BigDecimal.ZERO;
            for (Affiliate a : affiliateDAO.findAll()) {
                BigDecimal av = available.get(a.getAffiliateId());
                if (av == null) {
                    continue;
                }
                totalAvailable = totalAvailable.add(av);
                if (av.longValue() >= min && a.getBankAccount() != null && !Affiliate.SUSPENDED.equals(a.getStatus())) {
                    eligible.add(new Object[]{a, av, affiliateService.deduction(a, av)});
                }
            }
            request.setAttribute("payouts", model.Page.of(all, intParam(request, "page", 1), 10));
            request.setAttribute("eligible", eligible);
            request.setAttribute("totalAvailable", totalAvailable);
            request.setAttribute("q", q);
            request.setAttribute("statusFilter", status);
            request.setAttribute("reviewDays", affiliateService.reviewDays());
            request.setAttribute("payableDays", affiliateService.payableDays());
            request.setAttribute("payoutMin", min);
            request.setAttribute("withholdPct", affiliateService.withholdPct());
            request.setAttribute("withholdFrom", affiliateService.withholdFrom());
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load payouts", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        try {
            if ("settings".equals(action)) {
                int review = intParam(request, "reviewDays", -1);
                int payable = intParam(request, "payableDays", -1);
                int min = intParam(request, "payoutMin", -1);
                int pct = intParam(request, "withholdPct", -1);
                int from = intParam(request, "withholdFrom", -1);
                if (review < 0 || review > 60 || payable < review || payable > 120 || min < 0 || min > 50000000 || pct < 0 || pct > 50 || from < 0 || from > 50000000) {
                    redirect(request, response, "/admin/payouts", "bad");
                    return;
                }
                affiliateService.saveSettings(review, payable, min, pct, from);
                AuditService.log(actor, "AFFILIATE_SETTINGS", "SETTINGS", "affiliate", "payout_min", String.valueOf(min));
                redirect(request, response, "/admin/payouts", "settings");
                return;
            }
            if ("pay".equals(action)) {
                String ref = trim(request.getParameter("reference"));
                String[] ids = request.getParameterValues("affiliateId");
                if (ids == null || ids.length == 0 || !ref.matches("[A-Za-z0-9 _./-]{6,60}")) {
                    redirect(request, response, "/admin/payouts", "bad");
                    return;
                }
                List<Integer> list = new ArrayList<>();
                for (String s : ids) {
                    try {
                        list.add(Integer.parseInt(s));
                    } catch (NumberFormatException ignored) {
                    }
                }
                int n = payoutDAO.payBatch(list, ref, actor.getUserId(), affiliateService.withholdPct(), affiliateService.withholdFrom());
                AuditService.log(actor, "PAYOUT_BATCH", "PAYOUT", ref, "paid", String.valueOf(n));
                redirect(request, response, "/admin/payouts", n == 0 ? "nothing" : "paid");
                return;
            }
            redirect(request, response, "/admin/payouts", "bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update payouts", e);
        }
    }
}
