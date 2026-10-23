package controller.affiliate;

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
import model.Commission;
import model.Page;
import model.User;
import service.AffiliateService;
import service.AuditService;

@WebServlet(name = "CommissionServlet", urlPatterns = {"/admin/commissions"})
public class CommissionServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/commissions.jsp";

    private final CommissionDAO commissionDAO = new CommissionDAO();
    private final AffiliateService affiliateService = new AffiliateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            affiliateService.releaseMatured();
            String q = trim(request.getParameter("q"));
            String status = oneOf(trim(request.getParameter("status")), CommissionDAO.ALL_STATUSES);
            int days = Math.max(0, Math.min(365, intParam(request, "days", 0)));
            List<Commission> all = commissionDAO.search(q, status, null, days);
            if ("1".equals(request.getParameter("export"))) {
                List<String[]> rows = new ArrayList<>();
                for (Commission c : all) {
                    rows.add(new String[]{String.valueOf(c.getCommissionId()), c.getAffiliateName(), c.getOrderCode(), c.getOrderAmount().toPlainString(),
                        c.getEligibleAmount().toPlainString(), c.getRateText(), c.getAmount().toPlainString(), c.getStatus(), String.valueOf(c.getCreatedAt())});
                }
                csv(response, "commissions", new String[]{"ID", "Affiliate", "Order", "Order amount", "Eligible", "Rate", "Commission", "Status", "Created"}, rows);
                return;
            }
            request.setAttribute("result", Page.of(all, intParam(request, "page", 1), 10));
            request.setAttribute("totals", commissionDAO.totalsByStatus(null));
            request.setAttribute("bulkCount", commissionDAO.bulkApprovableCount(affiliateService.reviewDays()));
            request.setAttribute("reviewDays", affiliateService.reviewDays());
            request.setAttribute("q", q);
            request.setAttribute("statusFilter", status);
            request.setAttribute("daysFilter", days);
            request.setAttribute("statuses", CommissionDAO.ALL_STATUSES);
            int open = intParam(request, "id", 0);
            if (open > 0) {
                request.setAttribute("detail", commissionDAO.find(open));
            }
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load commissions", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        int id = intParam(request, "id", 0);
        String reason = trim(request.getParameter("reason"));
        String back = "/admin/commissions" + (id > 0 && !"bulk".equals(action) ? "?id=" + id : "");
        try {
            String msg = "blocked";
            if ("bulk".equals(action)) {
                int n = commissionDAO.bulkApprove(affiliateService.reviewDays(), affiliateService.payableDays());
                AuditService.log(actor, "COMMISSION_BULK_APPROVE", "COMMISSION", "bulk", "count", String.valueOf(n));
                redirect(request, response, "/admin/commissions", n == 0 ? "nothing" : "approved");
                return;
            }
            if ("approve".equals(action)) {
                msg = commissionDAO.approve(id, affiliateService.payableDays()) ? "approved" : "blocked";
            } else if ("reject".equals(action) || "hold".equals(action)) {
                if (reason.length() < 5 || reason.length() > 200) {
                    redirect(request, response, back, "bad");
                    return;
                }
                boolean ok = "reject".equals(action) ? commissionDAO.reject(id, reason) : commissionDAO.hold(id, reason);
                msg = ok ? ("reject".equals(action) ? "rejected" : "held") : "blocked";
            } else if ("adjust".equals(action)) {
                msg = adjust(request, id, reason, actor);
            }
            AuditService.log(actor, "COMMISSION_" + action.toUpperCase(), "COMMISSION", String.valueOf(id), "result", msg, "reason", reason);
            redirect(request, response, back, msg);
        } catch (SQLException e) {
            throw new ServletException("Cannot update the commission", e);
        }
    }

    private String adjust(HttpServletRequest request, int id, String reason, User actor) throws SQLException {
        Commission c = commissionDAO.find(id);
        if (c == null || reason.length() < 5 || reason.length() > 200) {
            return "bad";
        }
        String mode = oneOf(trim(request.getParameter("mode")), "full", "partial", "manual");
        long current = c.getAmount().longValue();
        long delta;
        try {
            if ("full".equals(mode)) {
                delta = -current;
            } else if ("partial".equals(mode)) {
                int pct = Integer.parseInt(trim(request.getParameter("value")));
                if (pct < 1 || pct > 100) {
                    return "bad";
                }
                delta = -BigDecimal.valueOf(current).multiply(BigDecimal.valueOf(pct)).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).longValue();
            } else if ("manual".equals(mode)) {
                delta = Long.parseLong(trim(request.getParameter("value")).replaceAll("[.,\\s]", ""));
                if (delta >= 0 || -delta > current) {
                    return "bad";
                }
            } else {
                return "bad";
            }
        } catch (NumberFormatException e) {
            return "bad";
        }
        return commissionDAO.adjust(id, delta, reason, actor.getUserId()) ? "adjusted" : "blocked";
    }
}
