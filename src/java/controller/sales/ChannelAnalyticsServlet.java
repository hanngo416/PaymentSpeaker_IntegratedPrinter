package controller.sales;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.ChannelAnalyticsService;

@WebServlet(name = "ChannelAnalyticsServlet", urlPatterns = {"/analytics"})
public class ChannelAnalyticsServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/analytics/overview.jsp";
    private final ChannelAnalyticsService analyticsService = new ChannelAnalyticsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String period = request.getParameter("period");
        String channel = request.getParameter("channel");
        String tab = request.getParameter("tab");
        if (tab == null || tab.trim().isEmpty()) {
            tab = "overview";
        }
        try {
            ChannelAnalyticsService.AnalyticsBundle bundle = analyticsService.getAnalytics(period, channel);
            request.setAttribute("bundle", bundle);
            request.setAttribute("activeTab", tab);
            request.setAttribute("period", bundle.period);
            request.setAttribute("channel", bundle.channel);
        } catch (SQLException e) {
            throw new ServletException("Cannot load channel analytics", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}

