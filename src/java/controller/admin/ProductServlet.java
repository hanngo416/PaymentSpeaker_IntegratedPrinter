package controller.admin;

import controller.auth.LoginServlet;
import dao.CatalogDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Group;
import model.Page;
import model.Product;
import model.User;
import service.AuditService;
import util.AppConfig;
import util.SqlErrors;

@WebServlet(name = "ProductServlet", urlPatterns = {"/admin/products"})
public class ProductServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/products.jsp";
    private static final int PAGE_SIZE = 10;
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("created", "Product added.");
        MESSAGES.put("updated", "Product updated.");
        MESSAGES.put("enabled", "Product is on sale again.");
        MESSAGES.put("disabled", "Product is hidden from the cashier.");
        MESSAGES.put("deleted", "Product deleted.");
        MESSAGES.put("used", "A product that is on an order cannot be deleted. Hide it instead.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final CatalogDAO catalogDAO = new CatalogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            String editId = request.getParameter("edit");
            if (editId != null && editId.matches("\\d{1,9}")) {
                Product p = catalogDAO.findProductById(Integer.parseInt(editId));
                if (p != null) {
                    request.setAttribute("form", p);
                    request.setAttribute("formPrice", p.getPrice().toBigInteger().toString());
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load the product", e);
        }
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        try {
            if ("save".equals(action)) {
                save(request, response, actor);
                return;
            }
            String msg = "bad";
            int id = Integer.parseInt(request.getParameter("id"));
            if ("enable".equals(action) || "disable".equals(action)) {
                boolean on = "enable".equals(action);
                if (catalogDAO.setProductActive(id, on)) {
                    AuditService.log(actor, "PRODUCT_STATUS", "PRODUCT", String.valueOf(id), "active", String.valueOf(on));
                    msg = on ? "enabled" : "disabled";
                }
            } else if ("delete".equals(action)) {
                if (catalogDAO.deleteUnusedProduct(id)) {
                    AuditService.log(actor, "PRODUCT_DELETE", "PRODUCT", String.valueOf(id));
                    msg = "deleted";
                } else {
                    msg = "used";
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin/products?msg=" + msg);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/products?msg=bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update the products", e);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Product p = new Product();

        String rawId = trim(request.getParameter("id"));
        boolean editing = !rawId.isEmpty();
        if (editing) {
            if (!rawId.matches("\\d{1,9}") || catalogDAO.findProductById(Integer.parseInt(rawId)) == null) {
                response.sendRedirect(request.getContextPath() + "/admin/products?msg=bad");
                return;
            }
            p.setProductId(Integer.parseInt(rawId));
        }

        String rawGroup = trim(request.getParameter("groupId"));
        if (rawGroup.matches("\\d{1,9}") && groupExists(Integer.parseInt(rawGroup))) {
            p.setGroupId(Integer.parseInt(rawGroup));
        } else {
            errors.put("groupId", "Choose a group.");
        }

        String code = trim(request.getParameter("productCode")).toUpperCase();
        if (!code.matches("[A-Z0-9_-]{2,30}")) {
            errors.put("productCode", "2-30 characters: letters, digits, dash or underscore.");
        }
        p.setProductCode(code);

        String name = trim(request.getParameter("productName"));
        if (name.length() < 2 || name.length() > 100) {
            errors.put("productName", "2-100 characters.");
        }
        p.setProductName(name);

        String rawPrice = trim(request.getParameter("price")).replaceAll("[.,\\s]", "");
        if (!rawPrice.matches("\\d{1,9}") || Long.parseLong(rawPrice) < 1 || Long.parseLong(rawPrice) > AppConfig.MAX_AMOUNT) {
            errors.put("price", "Enter a whole number from 1 to " + AppConfig.MAX_AMOUNT + " (VND).");
        } else {
            p.setPrice(new BigDecimal(rawPrice));
        }

        String rawOrder = trim(request.getParameter("displayOrder"));
        if (rawOrder.isEmpty()) {
            p.setDisplayOrder(0);
        } else if (rawOrder.matches("\\d{1,4}")) {
            p.setDisplayOrder(Integer.parseInt(rawOrder));
        } else {
            errors.put("displayOrder", "A number from 0 to 9999, or leave empty.");
        }

        p.setActive(request.getParameter("active") != null);

        if (errors.isEmpty()) {
            try {
                if (editing) {
                    catalogDAO.updateProduct(p);
                    AuditService.log(actor, "PRODUCT_UPDATE", "PRODUCT", code, "price", p.getPrice().toPlainString());
                } else {
                    catalogDAO.insertProduct(p);
                    AuditService.log(actor, "PRODUCT_CREATE", "PRODUCT", code, "price", p.getPrice().toPlainString());
                }
                response.sendRedirect(request.getContextPath() + "/admin/products?msg=" + (editing ? "updated" : "created"));
                return;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
                errors.put("productCode", "This code already exists.");
            }
        }
        request.setAttribute("errors", errors);
        request.setAttribute("form", p);
        request.setAttribute("formPrice", trim(request.getParameter("price")));
        request.setAttribute("formOrder", rawOrder);
        show(request, response);
    }

    private boolean groupExists(int groupId) throws SQLException {
        for (Group g : catalogDAO.findAllGroups()) {
            if (g.getGroupId() == groupId) {
                return true;
            }
        }
        return false;
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
            // no or bad page number: first page
        }
        String q = trim(request.getParameter("q"));
        String rawGroup = trim(request.getParameter("group"));
        Integer groupFilter = rawGroup.matches("\\d{1,9}") ? Integer.valueOf(rawGroup) : null;
        try {
            Page<Product> result = catalogDAO.searchProducts(q, groupFilter, page, PAGE_SIZE);
            if (result.getItems().isEmpty() && result.getTotal() > 0) {
                result = catalogDAO.searchProducts(q, groupFilter, result.getTotalPages(), PAGE_SIZE);
            }
            request.setAttribute("result", result);
            request.setAttribute("categories", catalogDAO.findAllCategories());
            request.setAttribute("groups", catalogDAO.findAllGroups());
        } catch (SQLException e) {
            throw new ServletException("Cannot load products", e);
        }
        request.setAttribute("q", q);
        request.setAttribute("groupFilter", groupFilter);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
