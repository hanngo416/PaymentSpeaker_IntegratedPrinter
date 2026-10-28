package controller.admin;

import controller.affiliate.AffiliateBase;
import dao.AffiliateDAO;
import dao.CommissionDAO;
import dao.PayoutDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Affiliate;
import model.Page;
import model.User;
import model.enums.Role;
import service.AffiliateService;
import service.AuditService;
import util.SqlErrors;

@WebServlet(name = "AffiliateServlet", urlPatterns = {"/admin/affiliates"})
public class AffiliateServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/admin/affiliates.jsp";
    private static final String VIEW_DETAIL = "/WEB-INF/views/admin/affiliate-detail.jsp";
    private static final int PAGE_SIZE = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AffiliateDAO affiliateDAO = new AffiliateDAO();
    private final CommissionDAO commissionDAO = new CommissionDAO();
    private final PayoutDAO payoutDAO = new PayoutDAO();
    private final AffiliateService affiliateService = new AffiliateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            String rawId = trim(request.getParameter("id"));
            if (!rawId.isEmpty()) {
                showDetail(request, response, intParam(request, "id", 0));
                return;
            }
            show(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load affiliates", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        try {
            if ("create".equals(action)) {
                create(request, response, actor);
                return;
            }
            int id = intParam(request, "id", 0);
            Affiliate a = affiliateDAO.find(id);
            if (a == null) {
                redirect(request, response, "/admin/affiliates", "missing");
                return;
            }
            String msg = "bad";
            if ("activate".equals(action) || "approve".equals(action)) {
                msg = affiliateDAO.setStatus(id, Affiliate.ACTIVE) ? "saved" : "bad";
                AuditService.log(actor, "AFFILIATE_STATUS", "AFFILIATE", String.valueOf(id), "status", Affiliate.ACTIVE);
            } else if ("suspend".equals(action)) {
                msg = affiliateDAO.setStatus(id, Affiliate.SUSPENDED) ? "saved" : "bad";
                AuditService.log(actor, "AFFILIATE_STATUS", "AFFILIATE", String.valueOf(id), "status", Affiliate.SUSPENDED);
            } else if ("delete".equals(action)) {
                if (affiliateDAO.deleteUnused(id)) {
                    AuditService.log(actor, "AFFILIATE_DELETE", "AFFILIATE", String.valueOf(id));
                    msg = "deleted";
                } else {
                    msg = "used";
                }
            } else if ("update".equals(action)) {
                update(request, response, actor, a);
                return;
            }
            String back = "detail".equals(request.getParameter("from")) ? "/admin/affiliates?id=" + id : "/admin/affiliates";
            redirect(request, response, back, msg);
        } catch (SQLException e) {
            throw new ServletException("Cannot update the affiliates", e);
        }
    }

    private void create(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Affiliate a = new Affiliate();
        String name = trim(request.getParameter("name"));
        if (!validName(name)) {
            errors.put("name", "2-60 characters, no control characters.");
        }
        a.setName(name);
        a.setEmail(trim(request.getParameter("email")));
        readProfile(request, a, errors);
        a.setStatus(Affiliate.ACTIVE);
        if (errors.isEmpty()) {
            try {
                a.setReferralCode(newCode(a));
                affiliateDAO.insert(a);
                AuditService.log(actor, "AFFILIATE_CREATE", "AFFILIATE", a.getReferralCode());
                redirect(request, response, "/admin/affiliates", "created");
                return;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
                errors.put("name", "This name or code already exists.");
            }
        }
        request.setAttribute("errors", errors);
        request.setAttribute("form", a);
        request.setAttribute("formRate", trim(request.getParameter("rate")));
        show(request, response);
    }

    private void update(HttpServletRequest request, HttpServletResponse response, User actor, Affiliate current)
            throws SQLException, ServletException, IOException {
        Map<String, String> errors = new HashMap<>();
        Affiliate a = new Affiliate();
        a.setAffiliateId(current.getAffiliateId());
        a.setReferralCode(current.getReferralCode());
        a.setStatus(current.getStatus());
        String name = trim(request.getParameter("name"));
        if (!validName(name)) {
            errors.put("name", "2-60 characters, no control characters.");
        }
        a.setName(name);
        a.setEmail(trim(request.getParameter("email")));
        String oldEmail = current.getEmail() == null ? "" : current.getEmail();
        String newEmail = a.getEmail() == null ? "" : a.getEmail();
        boolean emailChanged = !oldEmail.equalsIgnoreCase(newEmail);
        if (emailChanged && actor.getRole() != Role.ADMIN) {
            errors.put("email", "Only an administrator can change the affiliate email.");
        }
        readProfile(request, a, errors);
        if (errors.isEmpty()) {
            try {
                affiliateDAO.updateProfile(a);
                if (emailChanged) {
                    AuditService.log(actor, "AFFILIATE_EMAIL_CHANGE", "AFFILIATE", a.getReferralCode(), "from", oldEmail, "to", newEmail);
                }
                AuditService.log(actor, "AFFILIATE_UPDATE", "AFFILIATE", a.getReferralCode());
                redirect(request, response, "/admin/affiliates?id=" + a.getAffiliateId(), "saved");
                return;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
                errors.put("name", "This name already exists.");
            }
        }
        request.setAttribute("errors", errors);
        request.setAttribute("editForm", a);
        showDetail(request, response, current.getAffiliateId());
    }

    private void readProfile(HttpServletRequest request, Affiliate a, Map<String, String> errors) {
        String phone = trim(request.getParameter("phone"));
        if (!phone.isEmpty() && !phone.matches("[0-9 +]{8,15}")) {
            errors.put("phone", "8-15 digits, spaces or +.");
        }
        a.setPhone(phone);
        String email = a.getEmail() == null ? "" : a.getEmail();
        if (!email.isEmpty() && (email.length() > 100 || !email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))) {
            errors.put("email", "Enter a valid email.");
        }
        a.setEmail(email);
        String bank = trim(request.getParameter("bankName"));
        String account = trim(request.getParameter("bankAccount"));
        String holder = trim(request.getParameter("bankHolder"));
        boolean any = !bank.isEmpty() || !account.isEmpty() || !holder.isEmpty();
        if (any && (bank.length() < 2 || bank.length() > 60 || !account.matches("[0-9 ]{6,30}") || holder.length() < 3 || holder.length() > 100)) {
            errors.put("bank", "Fill the bank name, a numeric account (6-30 digits) and the account holder.");
        }
        a.setBankName(bank);
        a.setBankAccount(account);
        a.setBankHolder(holder.toUpperCase());
        BigDecimal rate = parseRate(trim(request.getParameter("rate")));
        if (rate == null) {
            errors.put("rate", "Enter a percent from 0 to 100 with up to 2 decimals.");
            rate = BigDecimal.ZERO;
        }
        a.setRate(rate);
    }

    private String newCode(Affiliate a) throws SQLException {
        String base = a.getName().toUpperCase().replaceAll("[^A-Z0-9]", "");
        base = base.isEmpty() ? "AFF" : base.substring(0, Math.min(6, base.length()));
        for (int i = 0; i < 30; i++) {
            String code = base + String.format("%02d", RANDOM.nextInt(100));
            if (affiliateDAO.findByCode(code) == null) {
                return code;
            }
        }
        return base + System.currentTimeMillis() % 100000;
    }

    private void show(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        String q = trim(request.getParameter("q"));
        String status = oneOf(trim(request.getParameter("status")), Affiliate.ACTIVE, Affiliate.PENDING, Affiliate.SUSPENDED);
        List<Affiliate> all = affiliateDAO.search(q, status);
        Page<Affiliate> result = Page.of(all, intParam(request, "page", 1), PAGE_SIZE);
        if ("1".equals(request.getParameter("export"))) {
            java.util.List<String[]> rows = new java.util.ArrayList<>();
            for (Affiliate a : all) {
                rows.add(new String[]{a.getReferralCode(), a.getName(), a.getStatus(), String.valueOf(a.getOrderCount()), a.getRevenue().toPlainString(), a.getCommissionTotal().toPlainString()});
            }
            csv(response, "affiliates", new String[]{"Code", "Name", "Status", "Orders", "Revenue", "Commission"}, rows);
            return;
        }
        request.setAttribute("result", result);
        request.setAttribute("q", q);
        request.setAttribute("statusFilter", status);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private void showDetail(HttpServletRequest request, HttpServletResponse response, int id)
            throws SQLException, ServletException, IOException {
        Affiliate a = affiliateDAO.find(id);
        if (a == null) {
            redirect(request, response, "/admin/affiliates", "missing");
            return;
        }
        request.setAttribute("a", a);
        request.setAttribute("wallet", affiliateService.wallet(id));
        request.setAttribute("commissions", commissionDAO.search(null, null, id, 0));
        request.setAttribute("payouts", payoutDAO.search(null, null, id));
        request.getRequestDispatcher(VIEW_DETAIL).forward(request, response);
    }

    static boolean validName(String name) {
        if (name.length() < 2 || name.length() > 60) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isISOControl(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static BigDecimal parseRate(String raw) {
        String s = trim(raw).replace(',', '.');
        if (s.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (!s.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            return null;
        }
        BigDecimal v = new BigDecimal(s);
        return v.compareTo(new BigDecimal("100")) > 0 ? null : v;
    }
}
