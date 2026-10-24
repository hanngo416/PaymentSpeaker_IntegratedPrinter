package controller.affiliate;

import dao.AffiliateDAO;
import dao.ChannelReportDAO;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import service.AuditService;

@WebServlet(name = "ChannelReportServlet", urlPatterns = {"/admin/channel-reports"})
public class ChannelReportServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/channel-reports.jsp";

    private final ChannelReportDAO dao = new ChannelReportDAO();
    private final AffiliateDAO affiliateDAO = new AffiliateDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String type = trim(request.getParameter("type"));
        if (!ChannelReportDAO.TYPES.containsKey(type)) {
            type = "sales_by_affiliate";
        }
        String channel = oneOf(trim(request.getParameter("channel")), "SHOPEE", "TIKTOK");
        String status = oneOf(trim(request.getParameter("status")), "PENDING", "APPROVED", "PAID", "REJECTED", "CANCELLED");
        int affId = intParam(request, "affiliateId", 0);
        java.sql.Date from = date(request.getParameter("from"));
        java.sql.Date to = date(request.getParameter("to"));
        try {
            ChannelReportDAO.Table table = dao.run(type, from, to, channel, affId > 0 ? affId : null, status);
            if ("1".equals(request.getParameter("export"))) {
                AuditService.log(user(request), "CHANNEL_REPORT_EXPORTED", "CHANNEL_REPORT", type, "rows", String.valueOf(table.rows.size()));
                csv(response, "platform-" + type.replace('_', '-'), table.header, table.rows);
                return;
            }
            request.setAttribute("canWrite", filter.RolePermissions.has(user(request).getRole(), model.enums.Permission.SALES_MANAGE));
            request.setAttribute("types", ChannelReportDAO.TYPES);
            request.setAttribute("type", type);
            request.setAttribute("title", ChannelReportDAO.TYPES.get(type));
            request.setAttribute("header", table.header);
            request.setAttribute("rows", Page.of(table.rows, intParam(request, "page", 1), 15));
            request.setAttribute("affiliates", affiliateDAO.findAll());
            request.setAttribute("channelFilter", channel);
            request.setAttribute("statusFilter", status);
            request.setAttribute("affiliateFilter", affId);
            request.setAttribute("fromFilter", trim(request.getParameter("from")));
            request.setAttribute("toFilter", trim(request.getParameter("to")));
            request.setAttribute("usesStatus", "sales_by_affiliate".equals(type) || "performance_by_date".equals(type));
            request.setAttribute("usesChannel", !"reconciliation".equals(type));
            request.getRequestDispatcher(VIEW).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot build the report", e);
        }
    }

    private static java.sql.Date date(String raw) {
        try {
            return raw == null || raw.trim().isEmpty() ? null : java.sql.Date.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
