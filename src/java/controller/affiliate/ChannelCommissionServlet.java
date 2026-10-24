package controller.affiliate;

import dao.AffiliateDAO;
import dao.ChannelCommissionDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import model.Affiliate;
import model.ChannelCommission;
import model.Page;
import model.User;
import model.enums.Permission;
import service.AuditService;
import service.ChannelCommissionService;

@WebServlet(name = "ChannelCommissionServlet", urlPatterns = {"/admin/channel-commissions", "/admin/channel-commissions/import", "/admin/channel-commissions/template"})
@MultipartConfig(maxFileSize = 2 * 1024 * 1024)
public class ChannelCommissionServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/channel-commissions.jsp";
    private static final String IMPORT_VIEW = "/WEB-INF/views/admin/channel-import.jsp";
    private static final String PREVIEW_KEY = "channelImportPreview";
    private static final int MAX_SHOWN = 50;
    private static final String TEMPLATE_SHOPEE
            = "order_id,commission_id,order_date,order_amount,commission_amount,commission_rate,affiliate\r\n"
            + "SPC-DEMO-1,,2026-10-01,400000,32000,8,LINH08\r\n"
            + "SPC-DEMO-2,CM-9002,2026-10-02,250000,12500,5,MINH05\r\n";
    private static final String TEMPLATE_TIKTOK
            = "order_id,commission_id,order_date,payment_amount,est_commission,commission_rate,creator_id\r\n"
            + "TTC-DEMO-1,,2026-10-01,600000,30000,5,MINH05\r\n"
            + "TTC-DEMO-2,CM-8002,2026-10-02,300000,24000,8,LINH08\r\n";

    private final ChannelCommissionDAO dao = new ChannelCommissionDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();
    private final ChannelCommissionService service = new ChannelCommissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.endsWith("/template")) {
            String channel = trim(request.getParameter("channel"));
            boolean tiktok = "TIKTOK".equals(channel);
            response.setContentType("text/csv");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + (tiktok ? "tiktok" : "shopee") + "-commission-template.csv\"");
            response.getOutputStream().write((tiktok ? TEMPLATE_TIKTOK : TEMPLATE_SHOPEE).getBytes("UTF-8"));
            return;
        }
        if (path.endsWith("/import")) {
            if (!canWrite(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            showImport(request, response, "SHOPEE", (ChannelCommissionService.Parsed) request.getSession().getAttribute(PREVIEW_KEY));
            return;
        }
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            String q = trim(request.getParameter("q"));
            String channel = oneOf(trim(request.getParameter("channel")), "SHOPEE", "TIKTOK");
            String status = oneOf(trim(request.getParameter("status")), "PENDING", "APPROVED", "PAID", "REJECTED", "CANCELLED");
            int affId = intParam(request, "affiliateId", 0);
            java.sql.Date from = date(request.getParameter("from"));
            java.sql.Date to = date(request.getParameter("to"));
            List<ChannelCommission> all = dao.search(q, channel, affId > 0 ? affId : null, status, from, to);
            if ("1".equals(request.getParameter("export"))) {
                List<String[]> rows = new ArrayList<>();
                for (ChannelCommission c : all) {
                    rows.add(new String[]{String.valueOf(c.getCommissionId()), c.getAffiliateName(), c.getChannel(), c.getPlatformOrderId(),
                        String.valueOf(c.getOrderDate()), c.getBaseAmount().toPlainString(), c.getCommissionType(), c.getCommissionRate().toPlainString(),
                        c.getCalculatedAmount().toPlainString(), c.getReportedAmount() == null ? "" : c.getReportedAmount().toPlainString(),
                        c.getApprovedAmount() == null ? "" : c.getApprovedAmount().toPlainString(), c.getStatus(), c.getDataSource(),
                        c.getAttribution(), c.getPayoutReference(), c.getRejectReason()});
                }
                csv(response, "platform-commissions", new String[]{"ID", "Affiliate", "Platform", "Platform order", "Order date", "Base amount",
                    "Type", "Rate", "Calculated", "Platform reported", "Approved", "Status", "Source", "Attribution", "Payout", "Reason"}, rows);
                return;
            }
            int id = intParam(request, "id", 0);
            if (id > 0) {
                request.setAttribute("detail", dao.find(id));
            }
            request.setAttribute("commissions", Page.of(all, intParam(request, "page", 1), 10));
            request.setAttribute("totals", dao.totalsByStatus());
            request.setAttribute("affiliates", affiliateDAO.findAll());
            request.setAttribute("canWrite", canWrite(request));
            request.setAttribute("q", q);
            request.setAttribute("channelFilter", channel);
            request.setAttribute("statusFilter", status);
            request.setAttribute("affiliateFilter", affId);
            request.setAttribute("fromFilter", trim(request.getParameter("from")));
            request.setAttribute("toFilter", trim(request.getParameter("to")));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load platform commissions", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        if (request.getServletPath().endsWith("/import")) {
            postImport(request, response, actor);
            return;
        }
        String action = trim(request.getParameter("action"));
        try {
            if ("create".equals(action)) {
                create(request, response, actor);
                return;
            }
            int id = intParam(request, "id", 0);
            String reason = trim(request.getParameter("reason"));
            String back = "/admin/channel-commissions?id=" + id;
            if ("approve".equals(action)) {
                boolean ok = dao.approve(id, actor.getUserId());
                if (ok) {
                    AuditService.log(actor, "CHANNEL_COMMISSION_APPROVED", "CHANNEL_COMMISSION", id);
                }
                redirect(request, response, back, ok ? "approved" : "blocked");
                return;
            }
            if ("reject".equals(action) || "cancel".equals(action)) {
                if (reason.length() < 5 || reason.length() > 200) {
                    redirect(request, response, back, "reason");
                    return;
                }
                boolean ok = "reject".equals(action) ? dao.reject(id, actor.getUserId(), reason) : dao.cancel(id, actor.getUserId(), reason);
                if (ok) {
                    AuditService.log(actor, "reject".equals(action) ? "CHANNEL_COMMISSION_REJECTED" : "CHANNEL_COMMISSION_CANCELLED",
                            "CHANNEL_COMMISSION", id, "reason", reason);
                }
                redirect(request, response, back, ok ? ("reject".equals(action) ? "rejected" : "cancelled") : "blocked");
                return;
            }
            redirect(request, response, "/admin/channel-commissions", "bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update the commission", e);
        }
    }

    private void create(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        int affId = intParam(request, "affiliateId", 0);
        Affiliate a = affId > 0 ? affiliateDAO.find(affId) : null;
        String channel = trim(request.getParameter("channel"));
        ChannelCommission c = new ChannelCommission();
        List<String> errors = ChannelCommissionService.fillManual(c, trim(request.getParameter("orderId")), trim(request.getParameter("orderDate")),
                trim(request.getParameter("base")), trim(request.getParameter("type")), trim(request.getParameter("rate")), a);
        if (!ChannelCommissionService.validChannel(channel)) {
            errors.add("Choose Shopee or TikTok Shop.");
        }
        String notes = trim(request.getParameter("notes"));
        if (notes.length() > 200) {
            errors.add("Notes can be at most 200 characters.");
        }
        c.setNotes(notes.isEmpty() ? null : notes);
        c.setChannel(channel);
        if (errors.isEmpty() && dao.existsByRef(channel, c.getExternalRef())) {
            errors.add("A commission for that platform order already exists.");
        }
        if (!errors.isEmpty()) {
            request.setAttribute("formErrors", errors);
            request.setAttribute("showForm", Boolean.TRUE);
            doGetForward(request, response);
            return;
        }
        c.setCreatedBy(actor.getUserId());
        int id = dao.insert(c);
        AuditService.log(actor, "CHANNEL_COMMISSION_CREATED", "CHANNEL_COMMISSION", id,
                "channel", channel, "order", c.getPlatformOrderId(), "amount", c.getCalculatedAmount().toPlainString());
        redirect(request, response, "/admin/channel-commissions?id=" + id, "created");
    }

    private void doGetForward(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            List<ChannelCommission> all = dao.search("", "", null, "", null, null);
            request.setAttribute("commissions", Page.of(all, 1, 10));
            request.setAttribute("totals", dao.totalsByStatus());
            request.setAttribute("affiliates", affiliateDAO.findAll());
            request.setAttribute("canWrite", canWrite(request));
            request.setAttribute("q", "");
            request.setAttribute("channelFilter", "");
            request.setAttribute("statusFilter", "");
            request.setAttribute("affiliateFilter", 0);
            request.setAttribute("fromFilter", "");
            request.setAttribute("toFilter", "");
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load platform commissions", e);
        }
    }

    private void postImport(HttpServletRequest request, HttpServletResponse response, User actor)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String action = trim(request.getParameter("action"));
        try {
            if ("confirm".equals(action)) {
                ChannelCommissionService.Parsed parsed = (ChannelCommissionService.Parsed) session.getAttribute(PREVIEW_KEY);
                String channel = (String) session.getAttribute(PREVIEW_KEY + "Channel");
                session.removeAttribute(PREVIEW_KEY);
                session.removeAttribute(PREVIEW_KEY + "Channel");
                if (parsed == null || channel == null || parsed.rows.isEmpty()) {
                    redirect(request, response, "/admin/channel-commissions/import", "nothing");
                    return;
                }
                ChannelCommissionService.Result r = service.commit(parsed, channel, actor.getUserId());
                AuditService.log(actor, "CHANNEL_COMMISSION_IMPORTED", "CHANNEL_COMMISSION", channel,
                        "imported", String.valueOf(r.imported), "duplicates", String.valueOf(r.duplicates));
                request.setAttribute("done", Boolean.TRUE);
                request.setAttribute("imported", r.imported);
                request.setAttribute("duplicates", r.duplicates);
                showImport(request, response, channel, null);
                return;
            }
            if ("discard".equals(action)) {
                session.removeAttribute(PREVIEW_KEY);
                session.removeAttribute(PREVIEW_KEY + "Channel");
                redirect(request, response, "/admin/channel-commissions/import", "cancelled");
                return;
            }
            String channel = trim(request.getParameter("channel"));
            if (!ChannelCommissionService.validChannel(channel)) {
                request.setAttribute("formError", "Choose Shopee or TikTok Shop.");
                showImport(request, response, "SHOPEE", null);
                return;
            }
            Part file;
            try {
                file = request.getPart("file");
            } catch (IllegalStateException e) {
                request.setAttribute("formError", "The file is larger than 2 MB.");
                showImport(request, response, channel, null);
                return;
            } catch (ServletException e) {
                request.setAttribute("formError", "Choose a CSV file to import.");
                showImport(request, response, channel, null);
                return;
            }
            if (file == null || file.getSize() == 0) {
                request.setAttribute("formError", "Choose a CSV file to import.");
                showImport(request, response, channel, null);
                return;
            }
            ChannelCommissionService.Parsed parsed;
            try (InputStream in = file.getInputStream()) {
                parsed = service.preview(in, channel);
            }
            if (parsed.errors.isEmpty() && parsed.rows.isEmpty()) {
                parsed.errors.add("Every row in the file was already imported. Nothing new to add.");
            }
            if (parsed.errors.isEmpty()) {
                session.setAttribute(PREVIEW_KEY, parsed);
                session.setAttribute(PREVIEW_KEY + "Channel", channel);
            } else {
                session.removeAttribute(PREVIEW_KEY);
                session.removeAttribute(PREVIEW_KEY + "Channel");
            }
            AuditService.log(actor, "CHANNEL_COMMISSION_PREVIEW", "CHANNEL_COMMISSION", channel,
                    "rows", String.valueOf(parsed.rows.size()), "errors", String.valueOf(parsed.errors.size()));
            showImport(request, response, channel, parsed);
        } catch (SQLException e) {
            throw new ServletException("Cannot import commissions", e);
        }
    }

    private void showImport(HttpServletRequest request, HttpServletResponse response, String channel,
            ChannelCommissionService.Parsed parsed) throws ServletException, IOException {
        request.setAttribute("channel", channel);
        if (parsed != null) {
            List<String> shown = new ArrayList<>(parsed.errors.subList(0, Math.min(MAX_SHOWN, parsed.errors.size())));
            request.setAttribute("errorCount", parsed.errors.size());
            request.setAttribute("errorLines", shown);
            request.setAttribute("previewRows", parsed.rows.subList(0, Math.min(MAX_SHOWN, parsed.rows.size())));
            request.setAttribute("previewCount", parsed.rows.size());
            request.setAttribute("duplicates", parsed.duplicates);
            request.setAttribute("hasPreview", Boolean.TRUE);
        }
        request.getRequestDispatcher(IMPORT_VIEW).forward(request, response);
    }

    private static boolean canWrite(HttpServletRequest request) {
        User u = user(request);
        return u != null && RolePermissions.has(u.getRole(), Permission.SALES_MANAGE);
    }

    private static java.sql.Date date(String raw) {
        try {
            return raw == null || raw.trim().isEmpty() ? null : java.sql.Date.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
