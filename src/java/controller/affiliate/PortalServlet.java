package controller.affiliate;

import dao.AffiliateDAO;
import dao.CampaignDAO;
import dao.CatalogDAO;
import dao.CommissionDAO;
import dao.PayoutDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Affiliate;
import model.Campaign;
import model.Commission;
import model.Product;
import model.User;
import service.AffiliateService;
import service.AuditService;

@WebServlet(name = "PortalServlet", urlPatterns = {"/affiliate/portal"})
public class PortalServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/portal.jsp";

    private final AffiliateDAO affiliateDAO = new AffiliateDAO();
    private final CommissionDAO commissionDAO = new CommissionDAO();
    private final CampaignDAO campaignDAO = new CampaignDAO();
    private final CatalogDAO catalogDAO = new CatalogDAO();
    private final PayoutDAO payoutDAO = new PayoutDAO();
    private final AffiliateService affiliateService = new AffiliateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            Affiliate me = affiliateDAO.findForEmail(user(request).getEmail());
            String tab = oneOf(trim(request.getParameter("tab")), "dashboard", "links", "catalog", "orders", "commissions", "wallet", "profile");
            request.setAttribute("tab", tab.isEmpty() ? "dashboard" : tab);
            if (me == null) {
                request.getRequestDispatcher(VIEW).forward(request, response);
                return;
            }
            Affiliate full = affiliateDAO.find(me.getAffiliateId());
            request.setAttribute("me", full);
            request.setAttribute("wallet", affiliateService.wallet(full.getAffiliateId()));
            request.setAttribute("totals", commissionDAO.totalsByStatus(full.getAffiliateId()));
            List<Commission> mine = commissionDAO.search(null, null, full.getAffiliateId(), 0);
            request.setAttribute("commissions", mine);
            request.setAttribute("payouts", payoutDAO.search(null, null, full.getAffiliateId()));
            request.setAttribute("payoutMin", affiliateService.payoutMin());
            request.setAttribute("openRequest", payoutDAO.hasOpenRequest(full.getAffiliateId()));
            if ("catalog".equals(request.getAttribute("tab"))) {
                request.setAttribute("catalog", catalog(full));
            }
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load the affiliate portal", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        try {
            Affiliate me = affiliateDAO.findForEmail(actor.getEmail());
            if (me == null) {
                redirect(request, response, "/affiliate/portal", "missing");
                return;
            }
            if ("bank".equals(action)) {
                String bank = trim(request.getParameter("bankName"));
                String account = trim(request.getParameter("bankAccount"));
                String holder = trim(request.getParameter("bankHolder")).toUpperCase();
                if (bank.length() < 2 || bank.length() > 60 || !account.matches("[0-9 ]{6,30}") || holder.length() < 3 || holder.length() > 100) {
                    redirect(request, response, "/affiliate/portal?tab=wallet", "bad");
                    return;
                }
                affiliateDAO.updateBank(me.getAffiliateId(), bank, account, holder);
                AuditService.log(actor, "AFFILIATE_BANK", "AFFILIATE", me.getReferralCode());
                redirect(request, response, "/affiliate/portal?tab=wallet", "saved");
                return;
            }
            if ("payout".equals(action)) {
                Affiliate full = affiliateDAO.find(me.getAffiliateId());
                try {
                    affiliateService.requestPayout(full);
                    AuditService.log(actor, "PAYOUT_REQUEST", "AFFILIATE", me.getReferralCode());
                    redirect(request, response, "/affiliate/portal?tab=wallet", "requested");
                } catch (IllegalArgumentException e) {
                    request.setAttribute("message", e.getMessage());
                    request.setAttribute("tab", "wallet");
                    request.setAttribute("me", full);
                    request.setAttribute("wallet", affiliateService.wallet(full.getAffiliateId()));
                    request.setAttribute("totals", commissionDAO.totalsByStatus(full.getAffiliateId()));
                    request.setAttribute("commissions", commissionDAO.search(null, null, full.getAffiliateId(), 0));
                    request.setAttribute("payouts", payoutDAO.search(null, null, full.getAffiliateId()));
                    request.setAttribute("payoutMin", affiliateService.payoutMin());
                    request.setAttribute("openRequest", payoutDAO.hasOpenRequest(full.getAffiliateId()));
                    request.getRequestDispatcher(VIEW).forward(request, response);
                }
                return;
            }
            redirect(request, response, "/affiliate/portal", "bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update the affiliate profile", e);
        }
    }

    private List<Object[]> catalog(Affiliate a) throws SQLException {
        List<Campaign> running = campaignDAO.findRunning();
        List<Object[]> rows = new ArrayList<>();
        for (Product p : catalogDAO.findActiveProducts()) {
            long price = p.getPrice().longValue();
            long best = 0;
            String rate = "";
            for (Campaign c : running) {
                if (price < c.getMinOrder().longValue()) {
                    continue;
                }
                long amount = c.commissionFor(price);
                if (amount > best) {
                    best = amount;
                    rate = c.rateText() + " (" + c.getName() + ")";
                }
            }
            rows.add(new Object[]{p, best, rate});
        }
        return rows;
    }
}
