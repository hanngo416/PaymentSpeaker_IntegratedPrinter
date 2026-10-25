package controller.admin;

import controller.auth.LoginServlet;
import dao.DiscountDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.DiscountCode;
import model.User;
import service.AuditService;
import util.SqlErrors;
import static util.Params.trim;

@WebServlet(name = "DiscountServlet", urlPatterns = {"/admin/discounts"})
public class DiscountServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/discounts.jsp";
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("created", "Discount code created.");
        MESSAGES.put("enabled", "Discount code enabled.");
        MESSAGES.put("disabled", "Discount code disabled.");
        MESSAGES.put("deleted", "Discount code deleted.");
        MESSAGES.put("used", "A code that was already used cannot be deleted. Disable it instead.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final DiscountDAO discountDAO = new DiscountDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        show(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String action = request.getParameter("action");
        try {
            if ("create".equals(action)) {
                create(request, response, actor);
                return;
            }
            String msg = "bad";
            int id = Integer.parseInt(request.getParameter("id"));
            if ("enable".equals(action) || "disable".equals(action)) {
                boolean on = "enable".equals(action);
                if (discountDAO.setActive(id, on)) {
                    AuditService.log(actor, "DISCOUNT_STATUS", "DISCOUNT", String.valueOf(id), "active", String.valueOf(on));
                    msg = on ? "enabled" : "disabled";
                }
            } else if ("delete".equals(action)) {
                if (discountDAO.deleteUnused(id)) {
                    AuditService.log(actor, "DISCOUNT_DELETE", "DISCOUNT", String.valueOf(id));
                    msg = "deleted";
                } else {
                    msg = "used";
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin/discounts?msg=" + msg);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/discounts?msg=bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update the discount codes", e);
        }
    }

    private void create(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        DiscountCode d = new DiscountCode();

        String code = trim(request.getParameter("code")).toUpperCase();
        if (!code.matches("[A-Z0-9_-]{3,30}")) {
            errors.put("code", "3-30 characters: letters, digits, dash or underscore.");
        }
        d.setCode(code);

        String kind = trim(request.getParameter("kind"));
        if (!DiscountCode.PERCENT.equals(kind) && !DiscountCode.FIXED.equals(kind)) {
            errors.put("kind", "Choose percent or fixed amount.");
        }
        d.setKind(kind);

        String rawValue = trim(request.getParameter("value")).replaceAll("[.,\\s]", "");
        if (!rawValue.matches("\\d{1,9}") || Long.parseLong(rawValue) <= 0) {
            errors.put("value", "Enter a whole number greater than 0.");
        } else if (DiscountCode.PERCENT.equals(kind) && Long.parseLong(rawValue) > 100) {
            errors.put("value", "A percent cannot be above 100.");
        } else {
            d.setValue(new BigDecimal(rawValue));
        }

        String holder = trim(request.getParameter("holder"));
        if (holder.length() > 100) {
            errors.put("holder", "At most 100 characters.");
        }
        d.setHolder(holder.isEmpty() ? null : holder);

        String rawMax = trim(request.getParameter("maxUses"));
        if (d.getHolder() != null) {
            d.setMaxUses(1);
        } else if (!rawMax.isEmpty()) {
            if (rawMax.matches("\\d{1,6}") && Integer.parseInt(rawMax) > 0) {
                d.setMaxUses(Integer.parseInt(rawMax));
            } else {
                errors.put("maxUses", "Leave empty for unlimited, or enter a number greater than 0.");
            }
        }

        try {
            d.setValidFrom(Date.valueOf(trim(request.getParameter("validFrom"))));
            d.setValidTo(Date.valueOf(trim(request.getParameter("validTo"))));
            if (d.getValidTo().before(d.getValidFrom())) {
                errors.put("validTo", "The end date cannot be before the start date.");
            }
        } catch (IllegalArgumentException e) {
            errors.put("validTo", "Enter both dates.");
        }

        if (errors.isEmpty()) {
            try {
                discountDAO.insert(d);
                AuditService.log(actor, "DISCOUNT_CREATE", "DISCOUNT", code,
                        "kind", kind, "value", d.getValue().toPlainString(), "holder", holder);
                response.sendRedirect(request.getContextPath() + "/admin/discounts?msg=created");
                return;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
                errors.put("code", "This code already exists.");
            }
        }
        request.setAttribute("errors", errors);
        request.setAttribute("form", d);
        request.setAttribute("formMaxUses", rawMax);
        request.setAttribute("formFrom", trim(request.getParameter("validFrom")));
        request.setAttribute("formTo", trim(request.getParameter("validTo")));
        request.setAttribute("formValue", trim(request.getParameter("value")));
        show(request, response);
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("codes", discountDAO.findAll());
        } catch (SQLException e) {
            throw new ServletException("Cannot load discount codes", e);
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
