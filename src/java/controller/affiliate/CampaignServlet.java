package controller.affiliate;

import dao.CampaignDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Campaign;
import model.Page;
import model.User;
import service.AuditService;
import util.SqlErrors;

@WebServlet(name = "CampaignServlet", urlPatterns = {"/admin/campaigns"})
public class CampaignServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/campaigns.jsp";
    private static final long MAX_VALUE = 50000000L;

    private final CampaignDAO campaignDAO = new CampaignDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            int edit = intParam(request, "edit", 0);
            if (edit > 0) {
                Campaign c = campaignDAO.find(edit);
                if (c != null && !"ENDED".equals(c.getStatus())) {
                    request.setAttribute("form", c);
                }
            }
            show(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load campaigns", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        try {
            if ("save".equals(action)) {
                save(request, response, actor);
                return;
            }
            int id = intParam(request, "id", 0);
            Campaign c = campaignDAO.find(id);
            if (c == null) {
                redirect(request, response, "/admin/campaigns", "missing");
                return;
            }
            String msg = "blocked";
            if ("publish".equals(action) || "resume".equals(action)) {
                if (c.getEndDate().toLocalDate().isBefore(LocalDate.now())) {
                    msg = "blocked";
                } else if (campaignDAO.setStatus(id, "ACTIVE", "DRAFT", "PAUSED")) {
                    msg = "saved";
                }
            } else if ("pause".equals(action) && campaignDAO.setStatus(id, "PAUSED", "ACTIVE")) {
                msg = "saved";
            } else if ("end".equals(action) && campaignDAO.setStatus(id, "ENDED", "ACTIVE", "PAUSED", "DRAFT")) {
                msg = "saved";
            } else if ("duplicate".equals(action)) {
                Campaign copy = duplicate(c);
                campaignDAO.insert(copy, actor.getUserId());
                msg = "created";
            } else if ("delete".equals(action)) {
                msg = campaignDAO.deleteUnused(id) ? "deleted" : "used";
            }
            AuditService.log(actor, "CAMPAIGN_" + action.toUpperCase(), "CAMPAIGN", String.valueOf(id), "result", msg);
            redirect(request, response, "/admin/campaigns", msg);
        } catch (SQLException e) {
            throw new ServletException("Cannot update campaigns", e);
        }
    }

    private Campaign duplicate(Campaign c) throws SQLException {
        Campaign copy = new Campaign();
        String base = c.getName().length() > 70 ? c.getName().substring(0, 70) : c.getName();
        String name = base + " (copy)";
        int n = 2;
        while (nameExists(name)) {
            name = base + " (copy " + n++ + ")";
        }
        copy.setName(name);
        copy.setDescription(c.getDescription());
        copy.setCommissionType(c.getCommissionType());
        copy.setCommissionValue(c.getCommissionValue());
        copy.setMinOrder(c.getMinOrder());
        copy.setStartDate(Date.valueOf(LocalDate.now()));
        copy.setEndDate(Date.valueOf(LocalDate.now().plusDays(30)));
        copy.setStatus("DRAFT");
        return copy;
    }

    private boolean nameExists(String name) throws SQLException {
        for (Campaign x : campaignDAO.search(name, "")) {
            if (x.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private void save(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Campaign c = new Campaign();
        int id = intParam(request, "id", 0);
        c.setCampaignId(id);
        String name = trim(request.getParameter("name"));
        if (name.length() < 3 || name.length() > 80) {
            errors.put("name", "3-80 characters.");
        }
        c.setName(name);
        String desc = trim(request.getParameter("description"));
        if (desc.length() > 300) {
            errors.put("description", "At most 300 characters.");
        }
        c.setDescription(desc);
        String type = oneOf(trim(request.getParameter("commissionType")), Campaign.PERCENT, Campaign.FIXED);
        if (type.isEmpty()) {
            errors.put("commissionType", "Choose percentage or fixed amount.");
        }
        c.setCommissionType(type);
        BigDecimal value = value(request, "commissionValue", type, errors, "commissionValue");
        c.setCommissionValue(value);
        if (!errors.containsKey("commissionValue") && value.signum() <= 0) {
            errors.put("commissionValue", "Enter a value above 0.");
        }
        BigDecimal min = money(trim(request.getParameter("minOrder")));
        if (min == null) {
            errors.put("minOrder", "Enter a whole VND amount from 0 to 50,000,000.");
            min = BigDecimal.ZERO;
        }
        c.setMinOrder(min);
        try {
            c.setStartDate(Date.valueOf(trim(request.getParameter("startDate"))));
            c.setEndDate(Date.valueOf(trim(request.getParameter("endDate"))));
            if (c.getEndDate().before(c.getStartDate())) {
                errors.put("endDate", "The end date cannot be before the start date.");
            }
        } catch (IllegalArgumentException e) {
            errors.put("startDate", "Enter a start and an end date.");
        }
        c.setStatus("DRAFT");
        if (errors.isEmpty()) {
            try {
                if (id > 0) {
                    if (!campaignDAO.update(c)) {
                        redirect(request, response, "/admin/campaigns", "blocked");
                        return;
                    }
                } else {
                    campaignDAO.insert(c, actor.getUserId());
                }
                AuditService.log(actor, id > 0 ? "CAMPAIGN_UPDATE" : "CAMPAIGN_CREATE", "CAMPAIGN", name);
                redirect(request, response, "/admin/campaigns", id > 0 ? "saved" : "created");
                return;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
                errors.put("name", "This name already exists.");
            }
        }
        request.setAttribute("errors", errors);
        request.setAttribute("form", c);
        request.setAttribute("formValue", trim(request.getParameter("commissionValue")));
        request.setAttribute("formMin", trim(request.getParameter("minOrder")));
        show(request, response);
    }

    private BigDecimal value(HttpServletRequest request, String param, String type, Map<String, String> errors, String key) {
        String raw = trim(request.getParameter(param)).replace(',', '.');
        if (raw.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (!raw.matches("\\d{1,8}(\\.\\d{1,2})?")) {
            errors.put(key, "Enter a number with up to 2 decimals.");
            return BigDecimal.ZERO;
        }
        BigDecimal v = new BigDecimal(raw);
        if (Campaign.PERCENT.equals(type) && v.compareTo(new BigDecimal("50")) > 0) {
            errors.put(key, "A percentage is at most 50.");
        } else if (v.compareTo(BigDecimal.valueOf(MAX_VALUE)) > 0) {
            errors.put(key, "At most 50,000,000.");
        }
        return v;
    }

    private static BigDecimal money(String raw) {
        String digits = raw.replaceAll("[.,\\s]", "");
        if (digits.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (!digits.matches("\\d{1,8}") || Long.parseLong(digits) > MAX_VALUE) {
            return null;
        }
        return new BigDecimal(digits);
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String q = trim(request.getParameter("q"));
        String status = oneOf(trim(request.getParameter("status")), "DRAFT", "ACTIVE", "PAUSED", "ENDED");
        Page<Campaign> result = Page.of(campaignDAO.search(q, status), intParam(request, "page", 1), 10);
        request.setAttribute("result", result);
        request.setAttribute("q", q);
        request.setAttribute("statusFilter", status);
        request.setAttribute("today", Date.valueOf(LocalDate.now()));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
