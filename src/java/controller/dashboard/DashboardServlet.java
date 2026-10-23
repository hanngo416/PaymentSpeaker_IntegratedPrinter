/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.dashboard;

import controller.auth.LoginServlet;
import dao.FraudAlertDAO;
import dao.TransactionDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.TxFilter;
import model.User;
import model.enums.Permission;
import model.view.DayTotal;
import model.view.TxRow;
import util.TxChart;

/**
 *
 * @author PC
 */
@WebServlet(name = "DashboardServlet", urlPatterns = {"/home", "/dashboard"})
public class DashboardServlet extends HttpServlet {

    private static final int DAYS = 7;
    private static final int RECENT_ROWS = 5;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final FraudAlertDAO alertDAO = new FraudAlertDAO();

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/dashboard".equals(request.getServletPath())) {
            response.sendRedirect(request.getContextPath() + "/home");
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        if (actor.getRole() == model.enums.Role.AFFILIATE) {
            response.sendRedirect(request.getContextPath() + "/affiliate/portal");
            return;
        }
        boolean canTransactions = RolePermissions.has(actor.getRole(), Permission.TRANSACTION_READ);
        boolean canAlerts = RolePermissions.has(actor.getRole(), Permission.ALERT_READ);
        request.setAttribute("canTransactions", canTransactions);
        request.setAttribute("canAlerts", canAlerts);
        try {
            if (canTransactions) {
                loadTransactions(request);
            }
            if (canAlerts) {
                Map<String, Integer> counts = alertDAO.countByStatus();
                Integer open = counts.get("NEW");
                request.setAttribute("newAlerts", open == null ? 0 : open);
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load the dashboard", e);
        }
        request.getRequestDispatcher("/WEB-INF/views/dashboard/home.jsp").forward(request, response);
    }

    private void loadTransactions(HttpServletRequest request) throws SQLException {
        long dayMillis = 24L * 60 * 60 * 1000;
        Date today = new Date(System.currentTimeMillis());
        TxFilter week = new TxFilter();
        week.setFrom(new Date(today.getTime() - (DAYS - 1) * dayMillis));
        week.setTo(today);

        List<DayTotal> days = transactionDAO.dailyTotals(week);
        BigDecimal validToday = BigDecimal.ZERO;
        BigDecimal flaggedToday = BigDecimal.ZERO;
        int countToday = 0;
        for (DayTotal d : days) {
            if (d.getDay().toString().equals(today.toString())) {
                validToday = d.getLegitTotal();
                flaggedToday = d.getFlaggedTotal();
                countToday = d.getCount();
            }
        }

        Page<TxRow> latest = transactionDAO.search(week);
        List<TxRow> recent = latest.getItems();
        if (recent.size() > RECENT_ROWS) {
            recent = recent.subList(0, RECENT_ROWS);
        }

        request.setAttribute("validToday", TxChart.money(validToday));
        request.setAttribute("flaggedToday", TxChart.money(flaggedToday));
        request.setAttribute("countToday", countToday);
        request.setAttribute("recent", recent);
        BigDecimal ceiling = TxChart.ceiling(days);
        request.setAttribute("bars", TxChart.bars(days, ceiling));
        request.setAttribute("ticks", TxChart.ticks(ceiling));
        request.setAttribute("chartWidth", TxChart.WIDTH);
        request.setAttribute("chartHeight", TxChart.HEIGHT);
        request.setAttribute("chartLeft", TxChart.LEFT);
        request.setAttribute("chartTop", TxChart.TOP);
        request.setAttribute("chartMax", TxChart.money(ceiling));
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
