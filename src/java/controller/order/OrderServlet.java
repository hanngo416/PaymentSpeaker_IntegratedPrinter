/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.order;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Order;
import model.User;
import service.OrderService;

/**
 *
 * @author PC
 */
@WebServlet(name = "OrderServlet", urlPatterns = {"/orders/new", "/orders/view", "/orders/cancel", "/orders/status"})
public class OrderServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(OrderServlet.class.getName());
    private static final String VIEW_NEW = "/WEB-INF/views/order/new.jsp";
    private static final String VIEW_ORDER = "/WEB-INF/views/order/view.jsp";

    private final OrderService orderService = new OrderService();

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
        User user = currentUser(request);
        try {
            switch (request.getServletPath()) {
                case "/orders/new":
                    showNewForm(request, response, user, null, null);
                    break;
                case "/orders/view":
                    showOrder(request, response, user);
                    break;
                case "/orders/status":
                    writeStatusJson(request, response, user);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            fail(response, e);
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = currentUser(request);
        try {
            switch (request.getServletPath()) {
                case "/orders/new":
                    createOrder(request, response, user);
                    break;
                case "/orders/cancel":
                    cancelOrder(request, response, user);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            fail(response, e);
        }
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

    private void showNewForm(HttpServletRequest request, HttpServletResponse response, User user,
            String error, String amount) throws SQLException, ServletException, IOException {
        request.setAttribute("recentOrders", orderService.recent(user));
        request.setAttribute("error", error);
        request.setAttribute("amount", amount);
        request.getRequestDispatcher(VIEW_NEW).forward(request, response);
    }

    private void createOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        String rawAmount = request.getParameter("amount");
        try {
            Order order = orderService.create(user, rawAmount);
            response.sendRedirect(request.getContextPath() + "/orders/view?code=" + order.getOrderCode());
        } catch (IllegalArgumentException | IllegalStateException e) {
            showNewForm(request, response, user, e.getMessage(), rawAmount);
        }
    }

    private void showOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Order order = findOrder(request, user);
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found.");
            return;
        }
        request.setAttribute("order", order);
        request.getRequestDispatcher(VIEW_ORDER).forward(request, response);
    }

    private void cancelOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        String code = request.getParameter("code");
        if (isValidCode(code)) {
            orderService.cancel(user, code);
            response.sendRedirect(request.getContextPath() + "/orders/view?code=" + code);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void writeStatusJson(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        Order order = findOrder(request, user);
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":\"" + order.getStatus().name()
                + "\",\"amount\":" + order.getAmount().longValue()
                + ",\"remaining\":" + order.getRemainingSeconds() + "}");
    }

    private Order findOrder(HttpServletRequest request, User user) throws SQLException {
        String code = request.getParameter("code");
        return isValidCode(code) ? orderService.findForUser(user, code) : null;
    }

    private static boolean isValidCode(String code) {
        return code != null && code.matches("[A-Z0-9]{3,30}");
    }

    private static User currentUser(HttpServletRequest request) {
        return (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
    }

    private void fail(HttpServletResponse response, SQLException e) throws IOException {
        LOG.log(Level.SEVERE, "Database error in OrderServlet", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}
