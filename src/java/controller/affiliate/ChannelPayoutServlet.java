package controller.affiliate;

import dao.AffiliateDAO;
import dao.ChannelPayoutDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.ChannelCommission;
import model.ChannelPayout;
import model.Page;
import model.User;
import model.enums.Permission;
import service.AuditService;

@WebServlet(name = "ChannelPayoutServlet", urlPatterns = {"/admin/channel-payouts"})
public class ChannelPayoutServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/channel-payouts.jsp";

    private final ChannelPayoutDAO dao = new ChannelPayoutDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            String q = trim(request.getParameter("q"));
            String status = oneOf(trim(request.getParameter("status")), "DRAFT", "CONFIRMED", "CANCELLED");
            int affId = intParam(request, "affiliateId", 0);
            List<ChannelPayout> all = dao.search(q, status, affId > 0 ? affId : null);
            if ("1".equals(request.getParameter("export"))) {
                List<String[]> rows = new ArrayList<>();
                for (ChannelPayout p : all) {
                    rows.add(new String[]{p.getReference(), p.getAffiliateName(), p.getTotalAmount().toPlainString(), p.getPaymentMethod(),
                        p.getPaymentReference(), String.valueOf(p.getPaymentDate() == null ? "" : p.getPaymentDate()), p.getStatus(),
                        p.getCreatedByName(), p.getConfirmedByName(), p.getNotes()});
                }
                csv(response, "platform-payouts", new String[]{"Payout", "Affiliate", "Total", "Method", "Payment reference", "Payment date",
                    "Status", "Created by", "Confirmed by", "Notes"}, rows);
                return;
            }
            int id = intParam(request, "id", 0);
            if (id > 0) {
                request.setAttribute("detail", dao.find(id));
            }
            Map<String, List<ChannelCommission>> groups = new LinkedHashMap<>();
            Map<String, java.math.BigDecimal> groupTotals = new LinkedHashMap<>();
            for (ChannelCommission c : dao.outstanding()) {
                String key = c.getAffiliateId() + "|" + c.getAffiliateName();
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
                groupTotals.merge(key, c.getPayableAmount(), java.math.BigDecimal::add);
            }
            request.setAttribute("groups", groups);
            request.setAttribute("groupTotals", groupTotals);
            request.setAttribute("payouts", Page.of(all, intParam(request, "page", 1), 10));
            request.setAttribute("affiliates", affiliateDAO.findAll());
            request.setAttribute("canWrite", canWrite(request));
            request.setAttribute("q", q);
            request.setAttribute("statusFilter", status);
            request.setAttribute("affiliateFilter", affId);
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load platform payouts", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        try {
            if ("draft".equals(action)) {
                int affId = intParam(request, "affiliateId", 0);
                String method = oneOf(trim(request.getParameter("method")), "BANK_TRANSFER", "CASH", "OTHER");
                String notes = trim(request.getParameter("notes"));
                String[] ids = request.getParameterValues("commissionId");
                List<Integer> list = new ArrayList<>();
                if (ids != null) {
                    for (String s : ids) {
                        try {
                            int v = Integer.parseInt(s);
                            if (!list.contains(v)) {
                                list.add(v);
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
                if (affId <= 0 || list.isEmpty() || notes.length() > 200) {
                    redirect(request, response, "/admin/channel-payouts", "bad");
                    return;
                }
                int id = dao.createDraft(affId, list, method.isEmpty() ? "BANK_TRANSFER" : method, notes.isEmpty() ? null : notes, actor.getUserId());
                if (id == 0) {
                    redirect(request, response, "/admin/channel-payouts", "stale");
                    return;
                }
                AuditService.log(actor, "CHANNEL_PAYOUT_CREATED", "CHANNEL_PAYOUT", id, "items", String.valueOf(list.size()));
                redirect(request, response, "/admin/channel-payouts?id=" + id, "draft");
                return;
            }
            int id = intParam(request, "id", 0);
            String back = "/admin/channel-payouts?id=" + id;
            if ("confirm".equals(action)) {
                String method = oneOf(trim(request.getParameter("method")), "BANK_TRANSFER", "CASH", "OTHER");
                String ref = trim(request.getParameter("paymentReference"));
                String notes = trim(request.getParameter("notes"));
                java.sql.Date date;
                try {
                    date = java.sql.Date.valueOf(trim(request.getParameter("paymentDate")));
                } catch (IllegalArgumentException e) {
                    date = null;
                }
                if (method.isEmpty() || !ref.matches("[A-Za-z0-9 _./-]{3,60}") || date == null
                        || date.after(new java.sql.Date(System.currentTimeMillis() + 86400000L)) || notes.length() > 200) {
                    redirect(request, response, back, "bad");
                    return;
                }
                boolean ok = dao.confirm(id, method, ref, date, notes.isEmpty() ? null : notes, actor.getUserId());
                if (ok) {
                    AuditService.log(actor, "CHANNEL_PAYOUT_CONFIRMED", "CHANNEL_PAYOUT", id, "method", method, "reference", ref);
                }
                redirect(request, response, back, ok ? "confirmed" : "blocked");
                return;
            }
            if ("cancel".equals(action)) {
                boolean ok = dao.cancel(id);
                if (ok) {
                    AuditService.log(actor, "CHANNEL_PAYOUT_CANCELLED", "CHANNEL_PAYOUT", id);
                }
                redirect(request, response, back, ok ? "cancelled" : "blocked");
                return;
            }
            if ("remove".equals(action)) {
                int cid = intParam(request, "commissionId", 0);
                boolean ok = dao.removeItem(id, cid);
                if (ok) {
                    AuditService.log(actor, "CHANNEL_PAYOUT_ITEM_REMOVED", "CHANNEL_PAYOUT", id, "commission", String.valueOf(cid));
                }
                redirect(request, response, back, ok ? "removed" : "blocked");
                return;
            }
            redirect(request, response, "/admin/channel-payouts", "bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update the platform payout", e);
        }
    }

    private static boolean canWrite(HttpServletRequest request) {
        User u = user(request);
        return u != null && RolePermissions.has(u.getRole(), Permission.SALES_MANAGE);
    }
}
