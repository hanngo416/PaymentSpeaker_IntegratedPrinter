package controller.admin;

import controller.auth.LoginServlet;
import dao.SettingDAO;
import dao.TaxRuleDAO;
import filter.RolePermissions;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.TaxRule;
import model.TaxRuleFilter;
import model.User;
import model.enums.Permission;
import service.AuditService;
import static util.Params.trim;

@WebServlet(name = "TaxRuleServlet", urlPatterns = {"/admin/taxes"})
public class TaxRuleServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/taxes.jsp";
    private static final List<String> KINDS = Arrays.asList(
            "THRESHOLD", "PIT_CEILING", "VAT_RATE", "PIT_RATE", "VAT_REDUCTION", "WARN_LEVEL");
    private static final List<String> SECTORS = Arrays.asList("GOODS", "SERVICE", "PRODUCTION", "OTHER");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal MAX_VALUE = new BigDecimal("99999999999999");
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("saved", "Tax rule saved.");
        MESSAGES.put("sector", "Business sector changed.");
        MESSAGES.put("enabled", "Tax rule enabled.");
        MESSAGES.put("disabled", "Tax rule disabled.");
        MESSAGES.put("deleted", "Tax rule deleted.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final TaxRuleDAO taxRuleDAO = new TaxRuleDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        String edit = request.getParameter("edit");
        if (edit != null && edit.matches("\\d{1,9}")) {
            try {
                TaxRule r = taxRuleDAO.findById(Integer.parseInt(edit));
                if (r != null) {
                    request.setAttribute("form", r);
                    request.setAttribute("formValue", r.getValue().stripTrailingZeros().toPlainString());
                    request.setAttribute("formFrom", String.valueOf(r.getEffectiveFrom()));
                    request.setAttribute("formTo", r.getEffectiveTo() == null ? "" : String.valueOf(r.getEffectiveTo()));
                }
            } catch (SQLException e) {
                throw new ServletException("Cannot load the tax rule", e);
            }
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
            if ("sector".equals(action)) {
                String sector = trim(request.getParameter("sector"));
                String msg = "bad";
                if (SECTORS.contains(sector)) {
                    String before = settingDAO.get("tax_sector", "GOODS");
                    settingDAO.set("tax_sector", sector);
                    AuditService.log(actor, "TAX_SECTOR_SET", "SETTING", "tax_sector", "from", before, "to", sector);
                    msg = "sector";
                }
                response.sendRedirect(request.getContextPath() + "/admin/taxes?msg=" + msg);
                return;
            }
            String msg = "bad";
            int id = Integer.parseInt(request.getParameter("id"));
            if ("toggle".equals(action)) {
                TaxRule r = taxRuleDAO.findById(id);
                if (r != null && taxRuleDAO.setActive(id, !r.isActive())) {
                    AuditService.log(actor, "TAX_RULE_TOGGLE", "TAX_RULE", String.valueOf(id),
                            "active", String.valueOf(!r.isActive()));
                    msg = r.isActive() ? "disabled" : "enabled";
                }
            } else if ("delete".equals(action)) {
                if (taxRuleDAO.delete(id)) {
                    AuditService.log(actor, "TAX_RULE_DELETE", "TAX_RULE", String.valueOf(id));
                    msg = "deleted";
                }
            }
            response.sendRedirect(request.getContextPath() + "/admin/taxes?msg=" + msg);
        } catch (NumberFormatException e) {
            response.sendRedirect(request.getContextPath() + "/admin/taxes?msg=bad");
        } catch (SQLException e) {
            throw new ServletException("Cannot update the tax rules", e);
        }
    }

    private void save(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        TaxRule r = new TaxRule();

        String rawId = trim(request.getParameter("id"));
        if (rawId.matches("\\d{1,9}")) {
            r.setRuleId(Integer.parseInt(rawId));
        }

        String kind = trim(request.getParameter("kind"));
        if (!KINDS.contains(kind)) {
            errors.put("kind", "Choose a valid kind.");
        }
        r.setKind(kind);

        String sector = trim(request.getParameter("sector"));
        boolean needsSector = "VAT_RATE".equals(kind) || "PIT_RATE".equals(kind);
        if (needsSector) {
            if (!SECTORS.contains(sector)) {
                errors.put("sector", "Choose a sector for this kind.");
            }
            r.setSector(sector);
        } else {
            r.setSector(null);
        }

        String rawValue = trim(request.getParameter("value")).replace(",", ".");
        BigDecimal value = null;
        try {
            value = new BigDecimal(rawValue);
        } catch (NumberFormatException e) {
            errors.put("value", "Enter a number.");
        }
        if (value != null) {
            if (value.signum() < 0) {
                errors.put("value", "The value cannot be negative.");
            } else if (("THRESHOLD".equals(kind) || "PIT_CEILING".equals(kind)) && value.signum() == 0) {
                errors.put("value", "The value must be greater than 0.");
            } else if (value.compareTo(MAX_VALUE) > 0 || value.scale() > 4) {
                errors.put("value", "The value is too large or has more than 4 decimals.");
            } else if (!"THRESHOLD".equals(kind) && !"PIT_CEILING".equals(kind) && value.compareTo(HUNDRED) > 0) {
                errors.put("value", "A percent cannot be above 100.");
            } else {
                r.setValue(value);
            }
        }

        Date from = null;
        try {
            from = Date.valueOf(trim(request.getParameter("effectiveFrom")));
            r.setEffectiveFrom(from);
        } catch (IllegalArgumentException e) {
            errors.put("effectiveFrom", "Enter the start date.");
        }
        String rawTo = trim(request.getParameter("effectiveTo"));
        if (!rawTo.isEmpty()) {
            try {
                Date to = Date.valueOf(rawTo);
                if (from != null && to.before(from)) {
                    errors.put("effectiveTo", "The end date cannot be before the start date.");
                } else {
                    r.setEffectiveTo(to);
                }
            } catch (IllegalArgumentException e) {
                errors.put("effectiveTo", "Enter a valid end date or leave it empty.");
            }
        }

        String source = trim(request.getParameter("sourceRef"));
        if (source.isEmpty() || source.length() > 200) {
            errors.put("sourceRef", "The source is required, at most 200 characters.");
        }
        r.setSourceRef(source);

        r.setVerified("1".equals(request.getParameter("verified")));
        r.setActive(true);

        if (errors.isEmpty()) {
            boolean isNew = r.getRuleId() == 0;
            boolean ok = true;
            if (isNew) {
                taxRuleDAO.insert(r);
            } else {
                ok = taxRuleDAO.update(r);
            }
            if (ok) {
                AuditService.log(actor, "TAX_RULE_SAVE", "TAX_RULE", isNew ? kind : String.valueOf(r.getRuleId()),
                        "kind", kind, "sector", sector, "value", r.getValue().toPlainString());
                response.sendRedirect(request.getContextPath() + "/admin/taxes?msg=saved");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin/taxes?msg=bad");
            }
            return;
        }
        request.setAttribute("errors", errors);
        request.setAttribute("form", r);
        request.setAttribute("formValue", trim(request.getParameter("value")));
        request.setAttribute("formFrom", trim(request.getParameter("effectiveFrom")));
        request.setAttribute("formTo", rawTo);
        show(request, response);
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        TaxRuleFilter f = new TaxRuleFilter();
        String kind = trim(request.getParameter("kind"));
        f.setKind(KINDS.contains(kind) ? kind : null);
        String q = trim(request.getParameter("q"));
        f.setQ(q.length() > 100 ? q.substring(0, 100) : q);
        String page = trim(request.getParameter("page"));
        f.setPage(page.matches("\\d{1,6}") && Integer.parseInt(page) > 0 ? Integer.parseInt(page) : 1);
        try {
            Page<TaxRule> result = taxRuleDAO.search(f);
            request.setAttribute("result", result);
            request.setAttribute("currentSector", settingDAO.get("tax_sector", "GOODS"));
        } catch (SQLException e) {
            throw new ServletException("Cannot load the tax rules", e);
        }
        request.setAttribute("kinds", KINDS);
        request.setAttribute("sectors", SECTORS);
        request.setAttribute("kindFilter", f.getKind() == null ? "" : f.getKind());
        request.setAttribute("q", f.getQ());
        request.setAttribute("canManage", RolePermissions.has(user.getRole(), Permission.TAX_MANAGE));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
