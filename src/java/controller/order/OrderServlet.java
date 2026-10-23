/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */
package controller.order;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import controller.auth.LoginServlet;
import dao.CatalogDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Order;
import model.OrderItem;
import model.User;
import service.OrderService;
import service.ShiftService;

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
    private final ShiftService shiftService = new ShiftService();
    private final CatalogDAO catalogDAO = new CatalogDAO();

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
                    if (!shiftOpen(request, response)) {
                        break;
                    }
                    showNewForm(request, response, user, null, null, null, null, null);
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
                    if (!shiftOpen(request, response)) {
                        break;
                    }
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

    private boolean shiftOpen(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        if (shiftService.current() != null) {
            return true;
        }
        response.sendRedirect(request.getContextPath() + "/shift?msg=noshift");
        return false;
    }

    private void showNewForm(HttpServletRequest request, HttpServletResponse response, User user,
            String error, String amount, String cashAmount, String pickedCode, String discountAmount) throws SQLException, ServletException, IOException {
        List<Order> recent = orderService.recent(user);
        BigDecimal storeToday = BigDecimal.ZERO;
        for (Order o : recent) {
            if ("COMPLETED".equals(o.getStatus())) {
                storeToday = storeToday.add(o.getAmount());
            }
        }
        request.setAttribute("recentOrders", recent);
        request.setAttribute("storeTodayTotal", storeToday);
        request.setAttribute("categories", catalogDAO.findAllCategories());
        request.setAttribute("groups", catalogDAO.findAllGroups());
        request.setAttribute("products", catalogDAO.findActiveProducts());
        request.setAttribute("error", error);
        request.setAttribute("amount", amount);
        request.setAttribute("cashAmount", cashAmount);
        request.setAttribute("sharedCodes", orderService.sharedCodes());
        request.setAttribute("pickedCode", pickedCode);
        request.setAttribute("discountAmount", discountAmount);
        request.setAttribute("channelToday", new dao.ChannelSaleDAO().todaySummaries());
        request.getRequestDispatcher(VIEW_NEW).forward(request, response);
    }

    private void createOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        String rawAmount = request.getParameter("amount");
        String rawCash = request.getParameter("cashAmount");
        String picked = request.getParameter("pickedCode");
        String rawDiscount = request.getParameter("discountAmount");
        String rawCartItems = request.getParameter("cartItems");
        String affiliateCode = request.getParameter("affiliateCode");
        boolean allCash = request.getParameter("methodCash") != null && request.getParameter("methodQr") == null;
        try {
            List<OrderItem> items = parseCartItems(rawCartItems);
            Order order = orderService.create(user, rawAmount, rawCash, allCash, picked, rawDiscount, items.isEmpty() ? null : items, affiliateCode);
            response.sendRedirect(request.getContextPath() + "/orders/view?code=" + order.getOrderCode());
        } catch (IllegalArgumentException | IllegalStateException e) {
            request.setAttribute("affiliateCode", affiliateCode);
            showNewForm(request, response, user, e.getMessage(), rawAmount, rawCash, picked, rawDiscount);
        }
    }

    private static List<OrderItem> parseCartItems(String rawJson) {
        List<OrderItem> items = new ArrayList<>();
        if (rawJson == null || rawJson.trim().isEmpty() || "[]".equals(rawJson.trim())) {
            return items;
        }
        try {
            JsonElement el = new JsonParser().parse(rawJson);
            if (!el.isJsonArray()) {
                return items;
            }
            JsonArray arr = el.getAsJsonArray();
            for (JsonElement itemEl : arr) {
                if (!itemEl.isJsonObject()) {
                    continue;
                }
                JsonObject obj = itemEl.getAsJsonObject();
                int pId = obj.has("productId") && !obj.get("productId").isJsonNull() ? obj.get("productId").getAsInt() : 0;
                int qty = obj.has("quantity") && !obj.get("quantity").isJsonNull() ? obj.get("quantity").getAsInt() : 1;
                items.add(new OrderItem(0, pId > 0 ? pId : null, "", BigDecimal.ZERO, qty, BigDecimal.ZERO));
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("The cart could not be read. Please rebuild it.");
        }
        return items;
    }

    private void showOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Order order = findOrder(request, user);
        if (order == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Order not found.");
            return;
        }
        request.setAttribute("order", order);
        request.setAttribute("orderItems", orderService.itemsOf(order.getOrderId()));
        request.getRequestDispatcher(VIEW_ORDER).forward(request, response);
    }

    private void cancelOrder(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, IOException {
        String code = request.getParameter("code");
        if (isValidCode(code)) {
            boolean cancelled = orderService.cancel(user, code);
            response.sendRedirect(request.getContextPath() + "/orders/view?code=" + code
                    + (cancelled ? "" : "&cancel=failed"));
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
