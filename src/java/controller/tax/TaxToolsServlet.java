package controller.tax;

import controller.affiliate.AffiliateBase;
import dao.SettingDAO;
import filter.RolePermissions;
import model.enums.Permission;
import dao.TaxToolsDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Page;
import model.User;
import service.AuditService;
import service.TaxService;
import service.TaxToolsService;

@WebServlet(name = "TaxToolsServlet", urlPatterns = {"/tax/profile", "/tax/calculator", "/tax/obligations", "/tax/payments",
    "/tax/expenses", "/tax/reconciliation", "/tax/notifications", "/tax/checker"})
public class TaxToolsServlet extends AffiliateBase {

    private static final String VIEW = "/WEB-INF/views/tax/";

    private final TaxToolsService tools = new TaxToolsService();
    private final TaxToolsDAO dao = new TaxToolsDAO();
    private final SettingDAO settingDAO = new SettingDAO();
    private final TaxService taxService = new TaxService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        try {
            switch (request.getServletPath()) {
                case "/tax/profile":
                    showProfile(request, response, null);
                    break;
                case "/tax/checker":
                    showChecker(request, response);
                    break;
                case "/tax/calculator":
                    showCalculator(request, response, null, null);
                    break;
                case "/tax/obligations":
                    showObligations(request, response, null);
                    break;
                case "/tax/payments":
                    showPayments(request, response, null);
                    break;
                case "/tax/expenses":
                    showExpenses(request, response, null);
                    break;
                case "/tax/reconciliation":
                    showReconciliation(request, response);
                    break;
                default:
                    showNotifications(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load the tax page", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = user(request);
        try {
            switch (request.getServletPath()) {
                case "/tax/profile":
                case "/tax/checker":
                    postProfile(request, response, actor);
                    break;
                case "/tax/calculator":
                    postCalculator(request, response, actor);
                    break;
                case "/tax/obligations":
                    postObligation(request, response, actor);
                    break;
                case "/tax/payments":
                    postPayment(request, response, actor);
                    break;
                case "/tax/expenses":
                    postExpense(request, response, actor);
                    break;
                case "/tax/reconciliation":
                    postRecon(request, response, actor);
                    break;
                case "/tax/notifications":
                    postNotification(request, response);
                    break;
                default:
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot update the tax data", e);
        }
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String page) throws ServletException, IOException {
        request.setAttribute("canManage", RolePermissions.has(user(request).getRole(), Permission.TAX_MANAGE));
        request.getRequestDispatcher(VIEW + page).forward(request, response);
    }

    private void showProfile(HttpServletRequest request, HttpServletResponse response, Map<String, String> errors) throws SQLException, ServletException, IOException {
        Map<String, String> saved = tools.profile();
        if (request.getAttribute("profile") == null) {
            request.setAttribute("profile", saved);
        }
        request.setAttribute("errors", errors);
        request.setAttribute("completion", tools.completion(saved));
        request.setAttribute("labels", TaxToolsService.LABELS);
        forward(request, response, "profile.jsp");
    }

    private void showChecker(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        Map<String, String> saved = tools.profile();
        request.setAttribute("completion", tools.completion(saved));
        request.setAttribute("checks", tools.check(saved));
        request.setAttribute("assessments", dao.assessments());
        forward(request, response, "checker.jsp");
    }

    private void postProfile(HttpServletRequest request, HttpServletResponse response, User actor) throws SQLException, ServletException, IOException {
        String action = trim(request.getParameter("action"));
        if ("confirm".equals(action)) {
            Map<String, String> saved = tools.profile();
            List<Map<String, Object>> checks = tools.check(saved);
            StringBuilder sb = new StringBuilder();
            boolean verified = true;
            for (Map<String, Object> c : checks) {
                sb.append(c.get("kind")).append(": ").append(c.get("status")).append("; ");
                verified &= Boolean.TRUE.equals(c.get("verified"));
            }
            String summary = sb.length() > 480 ? sb.substring(0, 480) : sb.toString();
            dao.addAssessment(Date.valueOf(LocalDate.now()), "Year " + LocalDate.now().getYear(), summary, verified ? "VERIFIED" : "UNVERIFIED", actor.getUserId());
            AuditService.log(actor, "TAX_ASSESSMENT", "TAX", String.valueOf(LocalDate.now().getYear()), "profile", String.valueOf(tools.completion(saved)));
            redirect(request, response, "/tax/checker", "saved");
            return;
        }
        Map<String, String> in = new LinkedHashMap<>();
        for (String f : TaxToolsService.FIELDS) {
            String v = trim(request.getParameter(f));
            if (request.getParameter(f + "_unknown") != null) {
                v = TaxToolsService.UNKNOWN;
            }
            in.put(f, v);
        }
        in.put("sector", oneOf(trim(request.getParameter("sector")), "GOODS", "SERVICE", "PRODUCTION", "OTHER"));
        Map<String, String> errors = tools.validate(in);
        if (errors.isEmpty()) {
            tools.saveProfile(in);
            AuditService.log(actor, "TAX_PROFILE_SAVE", "TAX", "profile", "completion", String.valueOf(tools.completion(in)));
            redirect(request, response, "/tax/profile", "saved");
            return;
        }
        request.setAttribute("profile", in);
        showProfile(request, response, errors);
    }

    private static final String[][] PERIODS = {{"Y", "Year to date"}, {"Q", "This quarter"}, {"PQ", "Previous quarter"}, {"M", "This month"}};

    private static LocalDate[] range(String code) {
        LocalDate today = LocalDate.now();
        LocalDate qStart = LocalDate.of(today.getYear(), (today.getMonthValue() - 1) / 3 * 3 + 1, 1);
        switch (code) {
            case "Q":
                return new LocalDate[]{qStart, today};
            case "PQ":
                return new LocalDate[]{qStart.minusMonths(3), qStart.minusDays(1)};
            case "M":
                return new LocalDate[]{today.withDayOfMonth(1), today};
            default:
                return new LocalDate[]{LocalDate.of(today.getYear(), 1, 1), today};
        }
    }

    private void showCalculator(HttpServletRequest request, HttpServletResponse response, Map<String, Object> result, String error) throws SQLException, ServletException, IOException {
        request.setAttribute("periods", PERIODS);
        request.setAttribute("sector", settingDAO.get("tax_sector", "GOODS"));
        List<Map<String, Object>> calcs = dao.calculations();
        request.setAttribute("calcs", calcs);
        request.setAttribute("result", result);
        request.setAttribute("error", error);
        int a = intParam(request, "a", 0);
        int b = intParam(request, "b", 0);
        if (a > 0 && b > 0) {
            request.setAttribute("cmpA", dao.calculation(a));
            request.setAttribute("cmpB", dao.calculation(b));
        }
        forward(request, response, "calculator.jsp");
    }

    private void postCalculator(HttpServletRequest request, HttpServletResponse response, User actor) throws SQLException, ServletException, IOException {
        String action = trim(request.getParameter("action"));
        String sector = settingDAO.get("tax_sector", "GOODS");
        try {
            if ("recalc".equals(action)) {
                Map<String, Object> c = dao.calculation(intParam(request, "id", 0));
                if (c == null) {
                    redirect(request, response, "/tax/calculator", "missing");
                    return;
                }
                Date from = (Date) c.get("period_from");
                Date to = (Date) c.get("period_to");
                BigDecimal revenue = (BigDecimal) c.get("revenue");
                BigDecimal adj = (BigDecimal) c.get("adjustment");
                Map<String, Object> r = tools.calculate((String) c.get("sector"), from, to, revenue, adj);
                saveCalc(actor, r, (String) c.get("sector"), (String) c.get("period_label"), from, to, revenue, adj, "Recalculated from #" + c.get("calc_id"));
                redirect(request, response, "/tax/calculator", Boolean.TRUE.equals(sameRules(c, r)) ? "nothing" : "saved");
                return;
            }
            String code = oneOf(trim(request.getParameter("period")), "Y", "Q", "PQ", "M");
            if (code.isEmpty()) {
                showCalculator(request, response, null, "Choose a period.");
                return;
            }
            LocalDate[] r = range(code);
            Date from = Date.valueOf(r[0]);
            Date to = Date.valueOf(r[1]);
            String rawRev = trim(request.getParameter("revenue")).replaceAll("[.,\\s]", "");
            BigDecimal revenue;
            if (rawRev.isEmpty()) {
                revenue = tools.revenue(from, to);
            } else if (rawRev.matches("\\d{1,13}")) {
                revenue = new BigDecimal(rawRev);
            } else {
                showCalculator(request, response, null, "Revenue must be a whole VND amount.");
                return;
            }
            String rawAdj = trim(request.getParameter("adjustment")).replaceAll("[.,\\s]", "");
            BigDecimal adj = rawAdj.isEmpty() ? BigDecimal.ZERO : rawAdj.matches("\\d{1,13}") ? new BigDecimal(rawAdj) : null;
            if (adj == null) {
                showCalculator(request, response, null, "The adjustment must be a whole VND amount.");
                return;
            }
            Map<String, Object> result = tools.calculate(sector, from, to, revenue, adj);
            String label = periodLabel(code, r);
            result.put("label", label);
            result.put("revenue", revenue);
            result.put("adjustment", adj);
            if ("save".equals(action)) {
                saveCalc(actor, result, sector, label, from, to, revenue, adj, trim(request.getParameter("note")));
                redirect(request, response, "/tax/calculator", "saved");
                return;
            }
            showCalculator(request, response, result, null);
        } catch (IllegalArgumentException e) {
            showCalculator(request, response, null, e.getMessage());
        }
    }

    private static String periodLabel(String code, LocalDate[] r) {
        for (String[] p : PERIODS) {
            if (p[0].equals(code)) {
                return p[1] + " (" + r[0] + " to " + r[1] + ")";
            }
        }
        return code;
    }

    private Boolean sameRules(Map<String, Object> old, Map<String, Object> fresh) {
        return old.get("rule_stamp").equals(fresh.get("stamp"));
    }

    private void saveCalc(User actor, Map<String, Object> r, String sector, String label, Date from, Date to, BigDecimal revenue, BigDecimal adj, String note) throws SQLException {
        String n = note.length() > 200 ? note.substring(0, 200) : note;
        dao.update("INSERT INTO TaxCalculations (calc_date, period_label, period_from, period_to, sector, revenue, adjustment, vat_amount, pit_amount, total_amount, rule_stamp, verified_rules, note, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Date.valueOf(LocalDate.now()), label.length() > 40 ? label.substring(0, 40) : label, from, to, sector, revenue, adj,
                r.get("vat"), r.get("pit"), ((BigDecimal) r.get("vat")).add((BigDecimal) r.get("pit")), r.get("stamp"), r.get("verified"), n.isEmpty() ? null : n, actor.getUserId());
        AuditService.log(actor, "TAX_CALC_SAVE", "TAX", label, "total", ((BigDecimal) r.get("vat")).add((BigDecimal) r.get("pit")).toPlainString());
    }

    private void showObligations(HttpServletRequest request, HttpServletResponse response, String error) throws SQLException, ServletException, IOException {
        request.setAttribute("rows", dao.obligations());
        request.setAttribute("statuses", TaxToolsDAO.OBLIGATION_STATUSES);
        request.setAttribute("error", error);
        int id = intParam(request, "id", 0);
        if (id > 0) {
            Map<String, Object> o = dao.obligation(id);
            request.setAttribute("detail", o);
            request.setAttribute("history", o == null ? null : dao.history(id));
            request.setAttribute("users", dao.query("SELECT user_id, full_name FROM Users WHERE status = N'ACTIVE' ORDER BY full_name"));
        }
        request.setAttribute("assessmentChecks", tools.check(tools.profile()));
        forward(request, response, "obligations.jsp");
    }

    private void postObligation(HttpServletRequest request, HttpServletResponse response, User actor) throws SQLException, ServletException, IOException {
        int id = intParam(request, "id", 0);
        Map<String, Object> o = dao.obligation(id);
        if (o == null) {
            redirect(request, response, "/tax/obligations", "missing");
            return;
        }
        String from = (String) o.get("status");
        String to = oneOf(trim(request.getParameter("status")), TaxToolsDAO.OBLIGATION_STATUSES);
        String note = trim(request.getParameter("note"));
        String ref = trim(request.getParameter("submissionRef"));
        BigDecimal declared = money(request.getParameter("declared"));
        BigDecimal confirmed = money(request.getParameter("confirmed"));
        String error = null;
        if (to.isEmpty()) {
            error = "Choose a status.";
        } else if (declared == BAD || confirmed == BAD) {
            error = "Amounts must be whole VND numbers.";
        } else if (!to.equals(from) && (note.length() < 3 || note.length() > 300)) {
            error = "Write a note of 3 to 300 characters when you change the status.";
        } else if ("PAID".equals(from) && !"PAID".equals(to) && !"NEEDS_ADJUSTMENT".equals(to)) {
            error = "A paid obligation can only move to Needs adjustment.";
        } else if ("SUBMITTED".equals(to) && ref.length() < 4) {
            error = "A submission reference of at least 4 characters is needed.";
        } else if ("AWAITING_PAYMENT".equals(to) && (confirmed == null || confirmed.signum() <= 0)) {
            error = "Enter the confirmed amount to pay before waiting for payment.";
        } else if ("PAID".equals(to)) {
            BigDecimal paid = (BigDecimal) o.get("paid_amount");
            if (confirmed == null || confirmed.signum() <= 0 || paid.compareTo(confirmed) < 0) {
                error = "There is no payment evidence covering the confirmed amount. Record the payment first.";
            }
        }
        if (error != null) {
            request.setAttribute("form", request.getParameterMap());
            showObligations(request, response, error);
            return;
        }
        Integer resp = intParam(request, "responsible", 0) > 0 ? intParam(request, "responsible", 0) : null;
        dao.changeObligation(id, from, to, resp, declared == null ? (BigDecimal) o.get("declared_amount") : declared,
                confirmed == null ? (BigDecimal) o.get("confirmed_amount") : confirmed, ref.isEmpty() ? (String) o.get("submission_ref") : ref, note, actor.getUserId());
        AuditService.log(actor, "TAX_OBLIGATION", "TAX", String.valueOf(id), "from", from, "to", to);
        redirect(request, response, "/tax/obligations?id=" + id, "saved");
    }

    private static final BigDecimal BAD = new BigDecimal("-1");

    private static BigDecimal money(String raw) {
        String s = raw == null ? "" : raw.trim().replaceAll("[.,\\s]", "");
        if (s.isEmpty()) {
            return null;
        }
        return s.matches("\\d{1,13}") ? new BigDecimal(s) : BAD;
    }

    private void showPayments(HttpServletRequest request, HttpServletResponse response, String error) throws SQLException, ServletException, IOException {
        List<Map<String, Object>> rows = new ArrayList<>();
        BigDecimal est = BigDecimal.ZERO;
        BigDecimal decl = BigDecimal.ZERO;
        BigDecimal conf = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (Map<String, Object> o : dao.obligations()) {
            if ("EINVOICE".equals(o.get("kind")) || "BOOKKEEPING".equals(o.get("kind"))) {
                continue;
            }
            BigDecimal c = o.get("confirmed_amount") == null ? BigDecimal.ZERO : (BigDecimal) o.get("confirmed_amount");
            BigDecimal p = (BigDecimal) o.get("paid_amount");
            o.put("outstanding", c.subtract(p).max(BigDecimal.ZERO));
            rows.add(o);
            est = est.add((BigDecimal) o.get("estimated_amount"));
            decl = decl.add(o.get("declared_amount") == null ? BigDecimal.ZERO : (BigDecimal) o.get("declared_amount"));
            conf = conf.add(c);
            paid = paid.add(p);
        }
        request.setAttribute("rows", rows);
        request.setAttribute("sumEst", est);
        request.setAttribute("sumDecl", decl);
        request.setAttribute("sumConf", conf);
        request.setAttribute("sumPaid", paid);
        request.setAttribute("sumOut", conf.subtract(paid).max(BigDecimal.ZERO));
        request.setAttribute("payments", dao.payments());
        request.setAttribute("error", error);
        forward(request, response, "payments.jsp");
    }

    private void postPayment(HttpServletRequest request, HttpServletResponse response, User actor) throws SQLException, ServletException, IOException {
        Map<String, Object> o = dao.obligation(intParam(request, "obligationId", 0));
        String error = null;
        BigDecimal amount = money(request.getParameter("amount"));
        String method = oneOf(trim(request.getParameter("method")), "Bank transfer", "Treasury counter", "E-wallet");
        String ref = trim(request.getParameter("reference"));
        String receipt = trim(request.getParameter("receipt"));
        Date on = null;
        try {
            on = Date.valueOf(trim(request.getParameter("paidOn")));
        } catch (IllegalArgumentException e) {
            error = "Enter the payment date.";
        }
        if (o == null) {
            error = "Obligation not found.";
        } else if (error == null) {
            BigDecimal confirmed = o.get("confirmed_amount") == null ? BigDecimal.ZERO : (BigDecimal) o.get("confirmed_amount");
            BigDecimal outstanding = confirmed.subtract((BigDecimal) o.get("paid_amount"));
            if (confirmed.signum() <= 0) {
                error = "This obligation has no confirmed amount to pay yet.";
            } else if (amount == null || amount == BAD || amount.signum() <= 0) {
                error = "Enter the amount paid as a whole VND number.";
            } else if (amount.compareTo(outstanding) > 0) {
                error = "The amount is above what is still owed (" + String.format("%,d", outstanding.longValue()) + " VND).";
            } else if (on.toLocalDate().isAfter(LocalDate.now())) {
                error = "The payment date cannot be in the future.";
            } else if (method.isEmpty()) {
                error = "Choose a payment method.";
            } else if (ref.length() < 6 && receipt.length() < 3) {
                error = "Evidence is required: a bank reference (6+ characters) or a receipt name.";
            } else if (ref.length() > 60 || receipt.length() > 100) {
                error = "Reference or receipt name is too long.";
            }
        }
        if (error != null) {
            showPayments(request, response, error);
            return;
        }
        dao.addPayment((Integer) o.get("obligation_id"), on, amount, method, ref.isEmpty() ? null : ref, receipt.isEmpty() ? null : receipt, actor.getUserId());
        AuditService.log(actor, "TAX_PAYMENT", "TAX", String.valueOf(o.get("obligation_id")), "amount", amount.toPlainString(), "reference", ref);
        redirect(request, response, "/tax/payments", "saved");
    }

    private void showExpenses(HttpServletRequest request, HttpServletResponse response, Map<String, String> errors) throws SQLException, ServletException, IOException {
        String q = trim(request.getParameter("q"));
        String cat = oneOf(trim(request.getParameter("category")), CATEGORIES);
        String doc = oneOf(trim(request.getParameter("doc")), "missing", "ok");
        Date from = date(request.getParameter("from"));
        Date to = date(request.getParameter("to"));
        List<Map<String, Object>> all = dao.expenses(q, cat, doc, from, to);
        if ("1".equals(request.getParameter("export"))) {
            List<String[]> rows = new ArrayList<>();
            for (Map<String, Object> e : all) {
                rows.add(new String[]{String.valueOf(e.get("spent_on")), (String) e.get("supplier"), (String) e.get("category"), String.valueOf(e.get("amount")),
                    (String) e.get("payment_method"), String.valueOf(e.get("invoice_no") == null ? "" : e.get("invoice_no")), Boolean.TRUE.equals(e.get("has_document")) ? "yes" : "no", (String) e.get("tax_treatment")});
            }
            csv(response, "expenses", new String[]{"Date", "Supplier", "Category", "Amount", "Payment", "Invoice", "Document", "Tax treatment"}, rows);
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        int missing = 0;
        for (Map<String, Object> e : all) {
            total = total.add((BigDecimal) e.get("amount"));
            if (!Boolean.TRUE.equals(e.get("has_document"))) {
                missing++;
            }
        }
        request.setAttribute("result", Page.of(all, intParam(request, "page", 1), 10));
        request.setAttribute("total", total);
        request.setAttribute("missing", missing);
        request.setAttribute("monthly", dao.expenseByMonth(5));
        request.setAttribute("q", q);
        request.setAttribute("catFilter", cat);
        request.setAttribute("docFilter", doc);
        request.setAttribute("from", from == null ? "" : from.toString());
        request.setAttribute("to", to == null ? "" : to.toString());
        request.setAttribute("categories", CATEGORIES);
        request.setAttribute("errors", errors);
        request.setAttribute("revenue30", tools.revenue(Date.valueOf(LocalDate.now().minusDays(29)), Date.valueOf(LocalDate.now())));
        request.setAttribute("expense30", dao.query("SELECT ISNULL(SUM(amount), 0) AS v FROM Expenses WHERE spent_on >= DATEADD(DAY, -29, CAST(GETDATE() AS DATE))").get(0).get("v"));
        forward(request, response, "expenses.jsp");
    }

    private static final String[] CATEGORIES = {"Ingredients", "Packaging", "Rent", "Utilities", "Salaries", "Marketing", "Equipment", "Other"};

    private static Date date(String raw) {
        try {
            return Date.valueOf(raw == null ? "" : raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void postExpense(HttpServletRequest request, HttpServletResponse response, User actor) throws SQLException, ServletException, IOException {
        String action = trim(request.getParameter("action"));
        int id = intParam(request, "id", 0);
        if ("delete".equals(action)) {
            redirect(request, response, "/tax/expenses", dao.deleteExpense(id) ? "deleted" : "missing");
            return;
        }
        if ("document".equals(action)) {
            dao.setExpenseDocument(id, "1".equals(request.getParameter("value")));
            redirect(request, response, "/tax/expenses", "saved");
            return;
        }
        if ("treatment".equals(action)) {
            String t = oneOf(trim(request.getParameter("value")), "UNCLASSIFIED", "MAY_COUNT", "NOT_COUNTED");
            List<Map<String, Object>> rows = dao.query("SELECT has_document FROM Expenses WHERE expense_id = ?", id);
            if (t.isEmpty() || rows.isEmpty()) {
                redirect(request, response, "/tax/expenses", "bad");
                return;
            }
            if ("MAY_COUNT".equals(t) && !Boolean.TRUE.equals(rows.get(0).get("has_document"))) {
                redirect(request, response, "/tax/expenses", "blocked");
                return;
            }
            dao.setExpenseTreatment(id, t);
            AuditService.log(actor, "EXPENSE_TREATMENT", "EXPENSE", String.valueOf(id), "treatment", t);
            redirect(request, response, "/tax/expenses", "saved");
            return;
        }
        Map<String, String> errors = new HashMap<>();
        Date on = date(request.getParameter("spentOn"));
        if (on == null || on.toLocalDate().isAfter(LocalDate.now())) {
            errors.put("spentOn", "Enter a date that is not in the future.");
        }
        String supplier = trim(request.getParameter("supplier"));
        if (supplier.length() < 2 || supplier.length() > 100) {
            errors.put("supplier", "2-100 characters.");
        }
        String taxCode = trim(request.getParameter("taxCode"));
        if (!taxCode.isEmpty() && !taxCode.matches("\\d{10}(-\\d{3})?")) {
            errors.put("taxCode", "10 digits, or 13 with a dash.");
        }
        String category = oneOf(trim(request.getParameter("category")), CATEGORIES);
        if (category.isEmpty()) {
            errors.put("category", "Choose a category.");
        }
        BigDecimal amount = money(request.getParameter("amount"));
        if (amount == null || amount == BAD || amount.signum() <= 0) {
            errors.put("amount", "A whole VND amount above 0.");
        }
        String method = oneOf(trim(request.getParameter("method")), "Bank transfer", "Cash", "Card");
        if (method.isEmpty()) {
            errors.put("method", "Choose a payment method.");
        }
        String invoice = trim(request.getParameter("invoiceNo"));
        if (invoice.length() > 40) {
            errors.put("invoiceNo", "At most 40 characters.");
        }
        if (errors.isEmpty()) {
            dao.addExpense(on, supplier, taxCode.isEmpty() ? null : taxCode, category, amount, method, invoice.isEmpty() ? null : invoice,
                    request.getParameter("hasDocument") != null, null, actor.getUserId());
            AuditService.log(actor, "EXPENSE_CREATE", "EXPENSE", supplier, "amount", amount.toPlainString());
            redirect(request, response, "/tax/expenses", "created");
            return;
        }
        showExpenses(request, response, errors);
    }

    private List<Map<String, Object>> reconRows() throws SQLException {
        Map<String, Map<String, Object>> notes = dao.reconNotes();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> r : dao.reconciliation(90)) {
            String kind;
            BigDecimal oa = (BigDecimal) r.get("order_amount");
            BigDecimal ia = (BigDecimal) r.get("invoice_amount");
            if (((Number) r.get("twins")).intValue() > 0) {
                kind = "POSSIBLE_DUPLICATE";
            } else if (r.get("invoice_id") == null) {
                kind = "MISSING_INVOICE";
            } else if (ia.compareTo(oa) != 0) {
                kind = "AMOUNT_DIFFERENCE";
            } else if (!"ISSUED".equals(r.get("invoice_status"))) {
                kind = "INVOICE_NOT_ISSUED";
            } else {
                kind = "MATCHED";
            }
            Map<String, Object> m = new LinkedHashMap<>();
            String key = "ORDER:" + r.get("order_id");
            m.put("key", key);
            m.put("kind", kind);
            m.put("ref", r.get("order_code"));
            m.put("posAmount", oa);
            m.put("invoiceAmount", ia);
            m.put("diff", ia == null ? oa : oa.subtract(ia));
            m.put("invoiceNo", r.get("invoice_no"));
            m.put("date", r.get("created_at"));
            Map<String, Object> n = notes.get(key);
            m.put("note", n == null ? "" : n.get("note"));
            m.put("resolved", n != null && Boolean.TRUE.equals(n.get("resolved")));
            out.add(m);
        }
        for (Map<String, Object> r : dao.orphanInvoices()) {
            Map<String, Object> m = new LinkedHashMap<>();
            String key = "INVOICE:" + r.get("invoice_id");
            m.put("key", key);
            m.put("kind", "UNMATCHED_INVOICE");
            m.put("ref", r.get("invoice_no"));
            m.put("posAmount", null);
            m.put("invoiceAmount", r.get("total_amount"));
            m.put("diff", r.get("total_amount"));
            m.put("invoiceNo", r.get("invoice_no"));
            m.put("date", null);
            Map<String, Object> n = notes.get(key);
            m.put("note", n == null ? "" : n.get("note"));
            m.put("resolved", n != null && Boolean.TRUE.equals(n.get("resolved")));
            out.add(m);
        }
        return out;
    }

    private void showReconciliation(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        List<Map<String, Object>> all = reconRows();
        String kind = trim(request.getParameter("kind"));
        String state = oneOf(trim(request.getParameter("state")), "open", "resolved");
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> r : all) {
            counts.merge((String) r.get("kind"), 1, Integer::sum);
            boolean match = kind.isEmpty() || kind.equals(r.get("kind"));
            if (match && ("open".equals(state) ? !"MATCHED".equals(r.get("kind")) && !Boolean.TRUE.equals(r.get("resolved")) : !"resolved".equals(state) || Boolean.TRUE.equals(r.get("resolved")))) {
                filtered.add(r);
            }
        }
        if ("1".equals(request.getParameter("export"))) {
            List<String[]> rows = new ArrayList<>();
            for (Map<String, Object> r : all) {
                rows.add(new String[]{(String) r.get("kind"), String.valueOf(r.get("ref")), String.valueOf(r.get("posAmount")), String.valueOf(r.get("invoiceAmount")), String.valueOf(r.get("diff")), Boolean.TRUE.equals(r.get("resolved")) ? "resolved" : "open", String.valueOf(r.get("note"))});
            }
            csv(response, "reconciliation", new String[]{"Result", "Reference", "POS amount", "Invoice amount", "Difference", "State", "Note"}, rows);
            return;
        }
        request.setAttribute("result", Page.of(filtered, intParam(request, "page", 1), 10));
        request.setAttribute("counts", counts);
        request.setAttribute("kindFilter", kind);
        request.setAttribute("stateFilter", state);
        forward(request, response, "reconciliation.jsp");
    }

    private void postRecon(HttpServletRequest request, HttpServletResponse response, User actor) throws SQLException, IOException {
        String key = trim(request.getParameter("key"));
        String note = trim(request.getParameter("note"));
        boolean resolved = "1".equals(request.getParameter("resolved"));
        if (!key.matches("(ORDER|INVOICE):\\d{1,9}") || note.length() < 3 || note.length() > 300) {
            redirect(request, response, "/tax/reconciliation", "bad");
            return;
        }
        dao.saveReconNote(key, note, resolved, actor.getUserId());
        AuditService.log(actor, "TAX_RECON_NOTE", "TAX", key, "resolved", String.valueOf(resolved));
        redirect(request, response, "/tax/reconciliation", "saved");
    }

    @SuppressWarnings("unchecked")
    private Set<String> readSet(HttpServletRequest request) {
        Object o = request.getSession().getAttribute("taxRead");
        if (o == null) {
            o = new TreeSet<String>();
            request.getSession().setAttribute("taxRead", o);
        }
        return (Set<String>) o;
    }

    private List<Map<String, Object>> notifications(HttpServletRequest request) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        Set<String> read = readSet(request);
        for (Map<String, Object> o : dao.obligations()) {
            String kind = (String) o.get("kind") + " " + o.get("period_label");
            if (((Number) o.get("late")).intValue() == 1) {
                list.add(note("overdue-" + o.get("obligation_id"), "OVERDUE", "Overdue: " + kind, "Was due on " + o.get("due_date") + ".", "/tax/obligations?id=" + o.get("obligation_id"), read));
            } else if (!"PAID".equals(o.get("status")) && !"NOT_APPLICABLE".equals(o.get("status")) && ((Date) o.get("due_date")).toLocalDate().isBefore(LocalDate.now().plusDays(8))) {
                list.add(note("due-" + o.get("obligation_id"), "DEADLINE", "Due soon: " + kind, "Due on " + o.get("due_date") + ".", "/tax/obligations?id=" + o.get("obligation_id"), read));
            }
        }
        int pc = tools.completion(tools.profile());
        if (pc < 100) {
            list.add(note("profile", "MISSING_INFO", "The tax profile is " + pc + "% complete", "Missing details keep obligations at Needs verification.", "/tax/profile", read));
        }
        int noDoc = dao.scalar("SELECT COUNT(*) FROM Expenses WHERE has_document = 0");
        if (noDoc > 0) {
            list.add(note("docs", "MISSING_DOCS", noDoc + " expense(s) have no supporting document", "Attach documents before treating them as deductible.", "/tax/expenses?doc=missing", read));
        }
        int unresolved = 0;
        for (Map<String, Object> r : reconRows()) {
            if (!"MATCHED".equals(r.get("kind")) && !Boolean.TRUE.equals(r.get("resolved"))) {
                unresolved++;
            }
        }
        if (unresolved > 0) {
            list.add(note("recon", "DISCREPANCY", unresolved + " reconciliation item(s) need review", "POS orders and invoices do not fully agree.", "/tax/reconciliation", read));
        }
        int unverified = dao.scalar("SELECT COUNT(*) FROM TaxRules WHERE active = 1 AND verified = 0");
        if (unverified > 0) {
            list.add(note("rules", "REGULATION", unverified + " active tax rule(s) are not verified", "Estimates based on them are marked as unverified.", "/admin/taxes", read));
        }
        TaxService.TaxView view = taxService.view(LocalDate.now().getYear());
        if (view.getEstimate() != null && view.getEstimate().getLevel() > 0) {
            list.add(note("threshold", "DEADLINE", "Revenue is at " + view.getEstimate().getPercent().toPlainString() + "% of the threshold", "Open the tax estimate for what to do next.", "/tax", read));
        }
        return list;
    }

    private static Map<String, Object> note(String key, String type, String title, String detail, String link, Set<String> read) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("type", type);
        m.put("title", title);
        m.put("detail", detail);
        m.put("link", link);
        m.put("read", read.contains(key));
        return m;
    }

    private void showNotifications(HttpServletRequest request, HttpServletResponse response) throws SQLException, ServletException, IOException {
        List<Map<String, Object>> all = notifications(request);
        String type = trim(request.getParameter("type"));
        boolean unreadOnly = "1".equals(request.getParameter("unread"));
        List<Map<String, Object>> shown = new ArrayList<>();
        int unread = 0;
        for (Map<String, Object> n : all) {
            if (!Boolean.TRUE.equals(n.get("read"))) {
                unread++;
            }
            if ((type.isEmpty() || type.equals(n.get("type"))) && (!unreadOnly || !Boolean.TRUE.equals(n.get("read")))) {
                shown.add(n);
            }
        }
        request.setAttribute("items", shown);
        request.setAttribute("unread", unread);
        request.setAttribute("typeFilter", type);
        request.setAttribute("unreadOnly", unreadOnly);
        forward(request, response, "notifications.jsp");
    }

    private void postNotification(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        String action = trim(request.getParameter("action"));
        Set<String> read = readSet(request);
        if ("all".equals(action)) {
            for (Map<String, Object> n : notifications(request)) {
                read.add((String) n.get("key"));
            }
        } else if ("read".equals(action)) {
            read.add(trim(request.getParameter("key")));
        } else if ("unread".equals(action)) {
            read.remove(trim(request.getParameter("key")));
        } else if ("open".equals(action)) {
            String key = trim(request.getParameter("key"));
            read.add(key);
            for (Map<String, Object> n : notifications(request)) {
                if (key.equals(n.get("key"))) {
                    response.sendRedirect(request.getContextPath() + n.get("link"));
                    return;
                }
            }
        }
        redirect(request, response, "/tax/notifications", "saved");
    }
}
