import controller.tax.TaxToolsServlet;
import dao.TaxToolsDAO;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import model.Page;
import model.User;
import model.enums.Role;
import util.DBUtil;

public class TaxObligationTest {

    private static int failures = 0;
    private static int passed = 0;
    private static final String TAG = "TOT" + System.currentTimeMillis();
    private static final TaxToolsDAO DAO = new TaxToolsDAO();
    private static final TaxToolsServlet SERVLET = new TaxToolsServlet();
    private static User actor;
    private static Map<String, Object> sessionAttrs = new HashMap<>();

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        } else {
            passed++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static String one(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    private static void run(String q, Object... args) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(q)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            ps.executeUpdate();
        }
    }

    private static int count(String q, Object... args) throws Exception {
        return Integer.parseInt(one(q, args));
    }

    private static class Call {
        Map<String, String[]> params = new LinkedHashMap<>();
        Map<String, Object> attrs = new HashMap<>();
        List<String> redirects = new ArrayList<>();
        String forwarded;
        int error = 0;
        StringWriter out = new StringWriter();
        String contentType;

        Call p(String k, String v) {
            params.put(k, new String[]{v});
            return this;
        }

        String redirect() {
            return redirects.isEmpty() ? null : redirects.get(0);
        }

        boolean redirectedWith(String msg) {
            return !redirects.isEmpty() && redirects.get(0).endsWith("msg=" + msg);
        }

        Object attr(String k) {
            return attrs.get(k);
        }
    }

    private static HttpSession session() {
        return (HttpSession) Proxy.newProxyInstance(TaxObligationTest.class.getClassLoader(), new Class<?>[]{HttpSession.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] a) {
                if (m.getName().equals("getAttribute")) {
                    return sessionAttrs.get((String) a[0]);
                }
                if (m.getName().equals("setAttribute")) {
                    sessionAttrs.put((String) a[0], a[1]);
                }
                return null;
            }
        });
    }

    private static Call call(String method, String path, Call c) throws Exception {
        final Call call = c;
        final HttpSession session = session();
        final String servletPath = path;
        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(TaxObligationTest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] a) {
                switch (m.getName()) {
                    case "getParameter": {
                        String[] v = call.params.get((String) a[0]);
                        return v == null ? null : v[0];
                    }
                    case "getParameterMap":
                        return call.params;
                    case "getSession":
                        return session;
                    case "getServletPath":
                        return servletPath;
                    case "getContextPath":
                        return "";
                    case "getAttribute":
                        return call.attrs.get((String) a[0]);
                    case "setAttribute":
                        call.attrs.put((String) a[0], a[1]);
                        return null;
                    case "getRequestDispatcher": {
                        final String target = (String) a[0];
                        return Proxy.newProxyInstance(TaxObligationTest.class.getClassLoader(), new Class<?>[]{RequestDispatcher.class}, new InvocationHandler() {
                            @Override
                            public Object invoke(Object p2, Method m2, Object[] a2) {
                                if (m2.getName().equals("forward")) {
                                    call.forwarded = target.substring(target.lastIndexOf('/') + 1);
                                }
                                return null;
                            }
                        });
                    }
                    default:
                        return null;
                }
            }
        });
        final PrintWriter writer = new PrintWriter(call.out);
        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(TaxObligationTest.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] a) {
                switch (m.getName()) {
                    case "sendRedirect":
                        call.redirects.add((String) a[0]);
                        return null;
                    case "sendError":
                        call.error = (Integer) a[0];
                        return null;
                    case "getWriter":
                        return writer;
                    case "setContentType":
                        call.contentType = (String) a[0];
                        return null;
                    default:
                        return null;
                }
            }
        });
        sessionAttrs.put("loginUser", actor);
        Method target = TaxToolsServlet.class.getDeclaredMethod(method, HttpServletRequest.class, HttpServletResponse.class);
        target.setAccessible(true);
        try {
            target.invoke(SERVLET, req, res);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception) {
                throw (Exception) e.getCause();
            }
            throw e;
        }
        writer.flush();
        return call;
    }

    private static Call post(String path, Call c) throws Exception {
        return call("doPost", path, c);
    }

    private static Call get(String path, Call c) throws Exception {
        return call("doGet", path, c);
    }

    private static Call obl(int id, String status, String note) {
        Call c = new Call().p("id", String.valueOf(id));
        if (status != null) {
            c.p("status", status);
        }
        if (note != null) {
            c.p("note", note);
        }
        return c;
    }

    private static String status(int id) throws Exception {
        return one("SELECT status FROM TaxObligations WHERE obligation_id = ?", id);
    }

    private static int historyCount(int id) throws Exception {
        return count("SELECT COUNT(*) FROM TaxObligationHistory WHERE obligation_id = ?", id);
    }

    private static int paymentCount(int id) throws Exception {
        return count("SELECT COUNT(*) FROM TaxPayments WHERE obligation_id = ?", id);
    }

    private static int newObligation(String label, String statusValue) throws Exception {
        run("INSERT INTO TaxObligations (kind, period_label, due_date, status, estimated_amount) VALUES ('VAT', ?, DATEADD(DAY, 30, CAST(GETDATE() AS DATE)), ?, 1000000)", label, statusValue);
        return count("SELECT obligation_id FROM TaxObligations WHERE period_label = ?", label);
    }

    private static Call pay(int id, String amount, String paidOn, String method, String ref, String receipt) {
        Call c = new Call().p("obligationId", String.valueOf(id)).p("amount", amount).p("paidOn", paidOn).p("method", method);
        if (ref != null) {
            c.p("reference", ref);
        }
        if (receipt != null) {
            c.p("receipt", receipt);
        }
        return c;
    }

    private static Call expense(String supplier, String date, String category, String amount, String method) {
        return new Call().p("supplier", supplier).p("spentOn", date).p("category", category).p("amount", amount).p("method", method);
    }

    private static int expenseId(String supplier) throws Exception {
        return count("SELECT expense_id FROM Expenses WHERE supplier = ?", supplier);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Call c) {
        return ((Page<Map<String, Object>>) c.attr("result")).getItems();
    }

    private static boolean dbRejects(String sql, Object... args) {
        try {
            run(sql, args);
            return false;
        } catch (Exception e) {
            return e instanceof SQLException;
        }
    }

    public static void main(String[] args) throws Exception {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        int maxCalc = Integer.parseInt(one("SELECT ISNULL(MAX(calc_id), 0) FROM TaxCalculations"));
        int maxAssess = Integer.parseInt(one("SELECT ISNULL(MAX(assessment_id), 0) FROM TaxAssessments"));
        try {
            actor = new User();
            actor.setUserId(Integer.parseInt(one("SELECT user_id FROM Users WHERE user_code = ?", "han_admin")));
            actor.setUserCode("han_admin");
            actor.setFullName(one("SELECT full_name FROM Users WHERE user_code = ?", "han_admin"));
            actor.setRole(Role.ADMIN);

            int id = newObligation(TAG + "-A", "NOT_ASSESSED");
            check("missing obligation redirects with msg=missing", post("/tax/obligations", obl(999999999, "PREPARATION", "note ok")).redirectedWith("missing"), true);

            Call c = post("/tax/obligations", obl(id, null, "note ok"));
            check("no status chosen shows the page with an error", c.forwarded + "|" + c.attr("error"), "obligations.jsp|Choose a status.");
            c = post("/tax/obligations", obl(id, "BOGUS", "note ok"));
            check("unknown status is refused", c.attr("error"), "Choose a status.");
            check("status unchanged after refused changes", status(id), "NOT_ASSESSED");

            c = post("/tax/obligations", obl(id, "PREPARATION", "ab"));
            check("note shorter than 3 refused", String.valueOf(c.attr("error")).startsWith("Write a note"), true);
            StringBuilder longNote = new StringBuilder();
            for (int i = 0; i < 301; i++) {
                longNote.append('x');
            }
            c = post("/tax/obligations", obl(id, "PREPARATION", longNote.toString()));
            check("note longer than 300 refused", String.valueOf(c.attr("error")).startsWith("Write a note"), true);
            check("no history after refused changes", historyCount(id), 0);

            c = obl(id, "PREPARATION", "start work").p("declared", "12abc");
            check("declared amount that is not whole VND refused", post("/tax/obligations", c).attr("error"), "Amounts must be whole VND numbers.");
            c = obl(id, "PREPARATION", "start work").p("confirmed", "-5");
            check("negative confirmed amount refused", post("/tax/obligations", c).attr("error"), "Amounts must be whole VND numbers.");

            c = post("/tax/obligations", obl(id, "PREPARATION", "start work").p("responsible", String.valueOf(actor.getUserId())).p("declared", "900.000"));
            check("valid change redirects to the detail page", c.redirect(), "/tax/obligations?id=" + id + "&msg=saved");
            check("status moved", status(id), "PREPARATION");
            check("one history row", historyCount(id), 1);
            check("responsible saved", one("SELECT responsible_user FROM TaxObligations WHERE obligation_id = ?", id), String.valueOf(actor.getUserId()));
            check("declared amount parsed with dots", new BigDecimal(one("SELECT declared_amount FROM TaxObligations WHERE obligation_id = ?", id)).toPlainString(), "900000.00");

            Map<String, Object> h = DAO.history(id).get(0);
            check("history from_status", h.get("from_status"), "NOT_ASSESSED");
            check("history to_status", h.get("to_status"), "PREPARATION");
            check("history note", h.get("note"), "start work");
            check("history records who", h.get("changed_by"), actor.getFullName());
            long age = System.currentTimeMillis() - ((java.sql.Timestamp) h.get("changed_at")).getTime();
            check("history records when (within 5 minutes)", Math.abs(age) < 300000, true);

            c = post("/tax/obligations", obl(id, "PREPARATION", null));
            check("same status without a note is allowed", c.redirectedWith("saved"), true);
            check("same status writes no history row", historyCount(id), 1);
            check("blank declared keeps the old value", new BigDecimal(one("SELECT declared_amount FROM TaxObligations WHERE obligation_id = ?", id)).toPlainString(), "900000.00");

            String[] walk = {"NEEDS_INFO", "APPLICABLE", "NOT_APPLICABLE", "READY_FOR_REVIEW", "OVERDUE", "NEEDS_ADJUSTMENT", "NOT_ASSESSED", "PREPARATION"};
            int expectedHistory = 1;
            for (String to : walk) {
                post("/tax/obligations", obl(id, to, "move to " + to));
                expectedHistory++;
                check("free transition to " + to, status(id), to);
            }
            check("history count after the walk", historyCount(id), expectedHistory);

            c = post("/tax/obligations", obl(id, "SUBMITTED", "send it"));
            check("SUBMITTED without reference refused", String.valueOf(c.attr("error")).startsWith("A submission reference"), true);
            c = post("/tax/obligations", obl(id, "SUBMITTED", "send it").p("submissionRef", "abc"));
            check("SUBMITTED with 3-character reference refused", String.valueOf(c.attr("error")).startsWith("A submission reference"), true);
            check("status still PREPARATION", status(id), "PREPARATION");
            c = post("/tax/obligations", obl(id, "SUBMITTED", "send it").p("submissionRef", "REF-" + TAG.substring(3, 9)));
            check("SUBMITTED with a valid reference accepted", status(id), "SUBMITTED");
            check("reference saved", one("SELECT submission_ref FROM TaxObligations WHERE obligation_id = ?", id), "REF-" + TAG.substring(3, 9));

            c = post("/tax/obligations", obl(id, "AWAITING_PAYMENT", "tax office answered"));
            check("AWAITING_PAYMENT without confirmed amount refused", String.valueOf(c.attr("error")).startsWith("Enter the confirmed amount"), true);
            c = post("/tax/obligations", obl(id, "AWAITING_PAYMENT", "tax office answered").p("confirmed", "0"));
            check("AWAITING_PAYMENT with confirmed 0 refused", String.valueOf(c.attr("error")).startsWith("Enter the confirmed amount"), true);
            c = post("/tax/obligations", obl(id, "AWAITING_PAYMENT", "tax office answered").p("confirmed", "1.000.000"));
            check("AWAITING_PAYMENT with confirmed amount accepted", status(id), "AWAITING_PAYMENT");
            check("confirmed amount saved", new BigDecimal(one("SELECT confirmed_amount FROM TaxObligations WHERE obligation_id = ?", id)).toPlainString(), "1000000.00");
            check("reference kept when the field is blank", one("SELECT submission_ref FROM TaxObligations WHERE obligation_id = ?", id), "REF-" + TAG.substring(3, 9));

            c = post("/tax/obligations", obl(id, "PAID", "paid in full").p("confirmed", "1000000"));
            check("PAID without any payment refused", String.valueOf(c.attr("error")).startsWith("There is no payment evidence"), true);
            check("still AWAITING_PAYMENT", status(id), "AWAITING_PAYMENT");

            c = post("/tax/payments", pay(id, "400000", today, "Bank transfer", "BANK1234", null));
            check("first part payment accepted", c.redirect(), "/tax/payments?msg=saved");
            check("one payment row", paymentCount(id), 1);
            c = post("/tax/obligations", obl(id, "PAID", "paid in full").p("confirmed", "1000000"));
            check("PAID with a part payment only refused", String.valueOf(c.attr("error")).startsWith("There is no payment evidence"), true);
            check("paid_amount after part payment", DAO.obligation(id).get("paid_amount").toString().replaceAll("\\.0+$", ""), "400000");

            c = post("/tax/payments", pay(id, "600001", today, "Bank transfer", "BANK5678", null));
            check("payment above the outstanding amount refused", String.valueOf(c.attr("error")).startsWith("The amount is above what is still owed (600,000"), true);
            check("overpayment writes no row", paymentCount(id), 1);
            c = post("/tax/payments", pay(id, "0", today, "Bank transfer", "BANK5678", null));
            check("zero amount refused", c.attr("error"), "Enter the amount paid as a whole VND number.");
            c = post("/tax/payments", pay(id, "12x", today, "Bank transfer", "BANK5678", null));
            check("non numeric amount refused", c.attr("error"), "Enter the amount paid as a whole VND number.");
            c = post("/tax/payments", pay(id, "1000", tomorrow, "Bank transfer", "BANK5678", null));
            check("future payment date refused", c.attr("error"), "The payment date cannot be in the future.");
            c = post("/tax/payments", pay(id, "1000", "", "Bank transfer", "BANK5678", null));
            check("missing payment date refused", c.attr("error"), "Enter the payment date.");
            c = post("/tax/payments", pay(id, "1000", today, "Bank transfer", null, null));
            check("no evidence refused", String.valueOf(c.attr("error")).startsWith("Evidence is required"), true);
            c = post("/tax/payments", pay(id, "1000", today, "Bank transfer", "12345", "ab"));
            check("short reference and short receipt refused", String.valueOf(c.attr("error")).startsWith("Evidence is required"), true);
            c = post("/tax/payments", pay(id, "1000", today, "Cheque", "BANK5678", null));
            check("unknown payment method refused", c.attr("error"), "Choose a payment method.");
            StringBuilder longRef = new StringBuilder();
            for (int i = 0; i < 61; i++) {
                longRef.append('r');
            }
            c = post("/tax/payments", pay(id, "1000", today, "Bank transfer", longRef.toString(), null));
            check("reference over 60 characters refused", c.attr("error"), "Reference or receipt name is too long.");
            c = post("/tax/payments", pay(999999999, "1000", today, "Bank transfer", "BANK5678", null));
            check("payment for a missing obligation refused", c.attr("error"), "Obligation not found.");
            check("no refused payment wrote a row", paymentCount(id), 1);

            c = post("/tax/payments", pay(id, "600000", today, "Treasury counter", null, "Receipt " + TAG));
            check("second part payment with a receipt name accepted", c.redirectedWith("saved"), true);
            check("two payment rows", paymentCount(id), 2);
            check("part payments add up to the confirmed amount", new BigDecimal(one("SELECT SUM(amount) FROM TaxPayments WHERE obligation_id = ?", id)).toPlainString(), "1000000.00");
            c = post("/tax/payments", pay(id, "1", today, "E-wallet", "WALLET01", null));
            check("a payment after full payment is refused", String.valueOf(c.attr("error")).startsWith("The amount is above what is still owed (0"), true);

            int fresh = newObligation(TAG + "-B", "PREPARATION");
            c = post("/tax/payments", pay(fresh, "1000", today, "Bank transfer", "BANK5678", null));
            check("payment on an obligation without a confirmed amount refused", c.attr("error"), "This obligation has no confirmed amount to pay yet.");

            c = post("/tax/obligations", obl(id, "PAID", "paid in full").p("confirmed", "1000000"));
            check("PAID accepted once payments cover the confirmed amount", status(id), "PAID");
            check("completed_on set when PAID", one("SELECT completed_on FROM TaxObligations WHERE obligation_id = ?", id), today);
            c = post("/tax/obligations", obl(id, "PAID", "paid in full"));
            check("PAID again without confirmed amount in the form refused", String.valueOf(c.attr("error")).startsWith("There is no payment evidence"), true);

            for (String bad : new String[]{"SUBMITTED", "PREPARATION", "AWAITING_PAYMENT", "NOT_APPLICABLE", "OVERDUE", "NEEDS_INFO"}) {
                post("/tax/obligations", obl(id, bad, "reopen it").p("submissionRef", "REF-OTHER1").p("confirmed", "1000000"));
                check("PAID cannot move to " + bad, status(id), "PAID");
            }
            c = post("/tax/obligations", obl(id, "PREPARATION", "reopen it"));
            check("PAID error text", c.attr("error"), "A paid obligation can only move to Needs adjustment.");
            c = post("/tax/obligations", obl(id, "NEEDS_ADJUSTMENT", "amount was wrong"));
            check("PAID can move to NEEDS_ADJUSTMENT", status(id), "NEEDS_ADJUSTMENT");
            Map<String, Object> last = DAO.history(id).get(0);
            check("last history row is PAID to NEEDS_ADJUSTMENT", last.get("from_status") + ">" + last.get("to_status"), "PAID>NEEDS_ADJUSTMENT");

            List<Map<String, Object>> hist = DAO.history(id);
            boolean sorted = true;
            for (int i = 1; i < hist.size(); i++) {
                sorted &= !((java.sql.Timestamp) hist.get(i - 1).get("changed_at")).before((java.sql.Timestamp) hist.get(i).get("changed_at"));
            }
            check("history is newest first", sorted, true);
            check("DAO history size equals row count", hist.size(), historyCount(id));

            check("payments() lists my payments", countMine(DAO.payments(), id), 2);
            check("obligations() reports paid_amount", paidOf(id), "1000000");
            check("obligations() flags late only when past due", lateOf(id), "0");
            run("UPDATE TaxObligations SET due_date = DATEADD(DAY, -3, CAST(GETDATE() AS DATE)), status = 'PREPARATION' WHERE obligation_id = ?", fresh);
            check("obligations() flags an overdue open obligation as late", lateOf(fresh), "1");
            run("UPDATE TaxObligations SET status = 'NOT_APPLICABLE' WHERE obligation_id = ?", fresh);
            check("NOT_APPLICABLE is never late", lateOf(fresh), "0");

            check("DB refuses a payment with no evidence", dbRejects("INSERT INTO TaxPayments (obligation_id, paid_on, amount, method, recorded_by) VALUES (?, CAST(GETDATE() AS DATE), 10, 'Bank transfer', ?)", id, actor.getUserId()), true);
            check("DB refuses a zero payment", dbRejects("INSERT INTO TaxPayments (obligation_id, paid_on, amount, method, reference, recorded_by) VALUES (?, CAST(GETDATE() AS DATE), 0, 'Bank transfer', 'REFREF', ?)", id, actor.getUserId()), true);
            check("DB refuses an unknown status", dbRejects("UPDATE TaxObligations SET status = 'WHATEVER' WHERE obligation_id = ?", fresh), true);

            run("INSERT INTO TaxCalculations (calc_date, period_label, period_from, period_to, sector, revenue, adjustment, vat_amount, pit_amount, total_amount, rule_stamp, verified_rules, note, created_by) VALUES (CAST(GETDATE() AS DATE), ?, '2026-01-01', '2026-03-31', 'GOODS', 500000000, 1000000, 111, 222, 333, ?, 1, ?, ?)",
                    TAG + "-calc", "STAMP-" + TAG, "note " + TAG, actor.getUserId());
            int calcId = count("SELECT calc_id FROM TaxCalculations WHERE period_label = ?", TAG + "-calc");
            Map<String, Object> calc = DAO.calculation(calcId);
            check("saved calculation re-read: rule stamp", calc.get("rule_stamp"), "STAMP-" + TAG);
            check("saved calculation re-read: total = vat + pit", ((BigDecimal) calc.get("total_amount")).toPlainString(), "333.00");
            check("saved calculation re-read: verified flag", calc.get("verified_rules"), true);
            check("saved calculation re-read: inputs", ((BigDecimal) calc.get("revenue")).toPlainString() + "/" + ((BigDecimal) calc.get("adjustment")).toPlainString(), "500000000.00/1000000.00");
            check("saved calculation re-read: sector and creator", calc.get("sector") + "/" + calc.get("created_by"), "GOODS/" + actor.getUserId());
            check("calculation of a missing id is null", DAO.calculation(999999999), null);
            check("calculations() lists it", countCalc(DAO.calculations(), calcId), 1);
            check("DB refuses adjustment above revenue", dbRejects("INSERT INTO TaxCalculations (calc_date, period_label, period_from, period_to, sector, revenue, adjustment, vat_amount, pit_amount, total_amount, rule_stamp, created_by) VALUES (CAST(GETDATE() AS DATE), ?, '2026-01-01', '2026-01-31', 'GOODS', 100, 200, 0, 0, 0, 'X', ?)", TAG + "-bad", actor.getUserId()), true);

            c = post("/tax/calculator", new Call().p("action", "save").p("period", "Y").p("revenue", "100.000.000").p("adjustment", "0").p("note", "note " + TAG));
            check("calculator save redirects", c.redirect(), "/tax/calculator?msg=saved");
            Map<String, Object> saved = DAO.query("SELECT TOP 1 * FROM TaxCalculations WHERE calc_id > ? AND note = ? ORDER BY calc_id DESC", maxCalc, "note " + TAG).get(0);
            check("calculator save keeps revenue", ((BigDecimal) saved.get("revenue")).toPlainString(), "100000000.00");
            check("calculator save keeps a rule stamp", String.valueOf(saved.get("rule_stamp")).trim().isEmpty(), false);
            check("calculator save total = vat + pit", ((BigDecimal) saved.get("total_amount")).compareTo(((BigDecimal) saved.get("vat_amount")).add((BigDecimal) saved.get("pit_amount"))), 0);
            check("calculator save records the user", saved.get("created_by"), actor.getUserId());
            int savedId = (Integer) saved.get("calc_id");

            c = post("/tax/calculator", new Call().p("action", "recalc").p("id", String.valueOf(savedId)));
            check("recalculating with unchanged rules says nothing new", c.redirect(), "/tax/calculator?msg=nothing");
            check("recalculation still stores a new row", count("SELECT COUNT(*) FROM TaxCalculations WHERE note = ?", "Recalculated from #" + savedId), 1);
            c = post("/tax/calculator", new Call().p("action", "recalc").p("id", "999999999"));
            check("recalculating a missing row redirects with missing", c.redirectedWith("missing"), true);

            int before = count("SELECT COUNT(*) FROM TaxCalculations");
            c = post("/tax/calculator", new Call().p("action", "save").p("period", "Y").p("revenue", "1000").p("adjustment", "5000").p("note", "note " + TAG));
            check("adjustment above revenue is refused by the calculator", c.forwarded + "|" + (c.attr("error") != null), "calculator.jsp|true");
            check("refused calculation writes no row", count("SELECT COUNT(*) FROM TaxCalculations"), before);
            c = post("/tax/calculator", new Call().p("action", "save").p("period", "Y").p("revenue", "12.5x"));
            check("non numeric revenue refused", c.attr("error"), "Revenue must be a whole VND amount.");
            c = post("/tax/calculator", new Call().p("action", "save").p("period", "ZZ").p("revenue", "1000"));
            check("unknown period refused", c.attr("error"), "Choose a period.");
            c = post("/tax/calculator", new Call().p("action", "preview").p("period", "Y").p("revenue", "100000000"));
            check("preview shows a result and writes nothing", (c.attr("result") != null) + "|" + count("SELECT COUNT(*) FROM TaxCalculations"), "true|" + (before));

            String assessPeriod = TAG + "-as";
            DAO.addAssessment(Date.valueOf(LocalDate.now()), assessPeriod, "VAT: APPLICABLE; ", "VERIFIED", actor.getUserId());
            List<Map<String, Object>> assessments = DAO.assessments();
            Map<String, Object> mine = null;
            for (Map<String, Object> a : assessments) {
                if (assessPeriod.equals(a.get("period_label"))) {
                    mine = a;
                }
            }
            check("saved assessment re-read", mine == null ? null : mine.get("summary") + "|" + mine.get("verification") + "|" + mine.get("created_name"), "VAT: APPLICABLE; |VERIFIED|" + actor.getFullName());
            check("newest assessment comes first", assessments.get(0).get("period_label"), assessPeriod);
            int beforeAssess = count("SELECT COUNT(*) FROM TaxAssessments");
            c = post("/tax/checker", new Call().p("action", "confirm"));
            check("checker confirm redirects", c.redirect(), "/tax/checker?msg=saved");
            check("checker confirm adds one assessment", count("SELECT COUNT(*) FROM TaxAssessments"), beforeAssess + 1);
            check("assessment verification is VERIFIED or UNVERIFIED", one("SELECT verification FROM TaxAssessments WHERE assessment_id > ?", maxAssess).matches("VERIFIED|UNVERIFIED"), true);

            Map<String, java.math.BigDecimal> monthBefore = monthTotals();
            c = post("/tax/expenses", expense(TAG + "A", today, "Ingredients", "10.000", "Cash").p("hasDocument", "on").p("invoiceNo", "INV-" + TAG));
            check("valid expense created", c.redirect(), "/tax/expenses?msg=created");
            LocalDate older = LocalDate.now().minusDays(40);
            post("/tax/expenses", expense(TAG + "B", older.toString(), "Rent", "20000", "Bank transfer"));
            post("/tax/expenses", expense(TAG + "C", LocalDate.now().minusDays(10).toString(), "Rent", "30000", "Card").p("taxCode", "0123456789-001"));
            check("three expenses stored", count("SELECT COUNT(*) FROM Expenses WHERE supplier LIKE ?", TAG + "%"), 3);
            check("document flag stored from the checkbox", one("SELECT has_document FROM Expenses WHERE supplier = ?", TAG + "A") + one("SELECT has_document FROM Expenses WHERE supplier = ?", TAG + "B"), "10");
            check("new expense is UNCLASSIFIED", one("SELECT tax_treatment FROM Expenses WHERE supplier = ?", TAG + "A"), "UNCLASSIFIED");
            check("expense records the creator", one("SELECT created_by FROM Expenses WHERE supplier = ?", TAG + "A"), String.valueOf(actor.getUserId()));

            int expBefore = count("SELECT COUNT(*) FROM Expenses");
            String[][] invalid = {
                {"supplier too short", "x", today, "Rent", "1000", "Cash", "supplier"},
                {"future date", TAG + "Z", tomorrow, "Rent", "1000", "Cash", "spentOn"},
                {"bad date", TAG + "Z", "31/12/2026", "Rent", "1000", "Cash", "spentOn"},
                {"unknown category", TAG + "Z", today, "Gold", "1000", "Cash", "category"},
                {"zero amount", TAG + "Z", today, "Rent", "0", "Cash", "amount"},
                {"text amount", TAG + "Z", today, "Rent", "abc", "Cash", "amount"},
                {"unknown method", TAG + "Z", today, "Rent", "1000", "Barter", "method"}
            };
            for (String[] row : invalid) {
                c = post("/tax/expenses", expense(row[1], row[2], row[3], row[4], row[5]));
                @SuppressWarnings("unchecked")
                Map<String, String> errors = (Map<String, String>) c.attr("errors");
                check("expense " + row[0] + " refused", c.forwarded + "|" + (errors != null && errors.containsKey(row[6])), "expenses.jsp|true");
            }
            c = post("/tax/expenses", expense(TAG + "Z", today, "Rent", "1000", "Cash").p("taxCode", "123"));
            check("bad tax code refused", ((Map<?, ?>) c.attr("errors")).containsKey("taxCode"), true);
            StringBuilder longSup = new StringBuilder();
            for (int i = 0; i < 101; i++) {
                longSup.append('s');
            }
            c = post("/tax/expenses", expense(longSup.toString(), today, "Rent", "1000", "Cash"));
            check("supplier over 100 characters refused", ((Map<?, ?>) c.attr("errors")).containsKey("supplier"), true);
            c = post("/tax/expenses", expense(TAG + "Z", today, "Rent", "1000", "Cash").p("invoiceNo", longSup.substring(0, 41)));
            check("invoice number over 40 characters refused", ((Map<?, ?>) c.attr("errors")).containsKey("invoiceNo"), true);
            check("refused expenses wrote no row", count("SELECT COUNT(*) FROM Expenses"), expBefore);

            c = get("/tax/expenses", new Call().p("q", TAG));
            check("search by supplier prefix", items(c).size(), 3);
            c = get("/tax/expenses", new Call().p("q", "INV-" + TAG));
            check("search by invoice number", items(c).size() + "|" + items(c).get(0).get("supplier"), "1|" + TAG + "A");
            c = get("/tax/expenses", new Call().p("q", TAG).p("category", "Rent"));
            check("filter by category", items(c).size(), 2);
            c = get("/tax/expenses", new Call().p("q", TAG).p("category", "Gold"));
            check("unknown category filter is ignored", items(c).size(), 3);
            c = get("/tax/expenses", new Call().p("q", TAG).p("doc", "missing"));
            check("filter missing document", items(c).size(), 2);
            check("missing-document warning count is the filtered number", c.attr("missing"), 2);
            c = get("/tax/expenses", new Call().p("q", TAG).p("doc", "ok"));
            check("filter with document", items(c).size() + "|" + items(c).get(0).get("supplier"), "1|" + TAG + "A");
            c = get("/tax/expenses", new Call().p("q", TAG));
            check("missing-document warning count unfiltered", c.attr("missing"), 2);
            check("total of the listed expenses", ((BigDecimal) c.attr("total")).toPlainString(), "60000.00");
            c = get("/tax/expenses", new Call().p("q", TAG).p("from", LocalDate.now().minusDays(15).toString()));
            check("filter from date", items(c).size(), 2);
            c = get("/tax/expenses", new Call().p("q", TAG).p("to", LocalDate.now().minusDays(15).toString()));
            check("filter to date", items(c).size() + "|" + items(c).get(0).get("supplier"), "1|" + TAG + "B");
            c = get("/tax/expenses", new Call().p("q", TAG).p("from", LocalDate.now().minusDays(10).toString()).p("to", LocalDate.now().minusDays(10).toString()));
            check("date range inclusive on both ends", items(c).size() + "|" + items(c).get(0).get("supplier"), "1|" + TAG + "C");
            c = get("/tax/expenses", new Call().p("q", TAG).p("category", "Rent").p("doc", "missing").p("from", LocalDate.now().minusDays(15).toString()));
            check("filters combine", items(c).size() + "|" + items(c).get(0).get("supplier"), "1|" + TAG + "C");
            check("newest expense first", items(get("/tax/expenses", new Call().p("q", TAG))).get(0).get("supplier"), TAG + "A");
            c = get("/tax/expenses", new Call().p("q", "no-such-supplier-" + TAG));
            check("search with no match is empty", items(c).size(), 0);

            Map<String, BigDecimal> monthAfter = monthTotals();
            Map<String, BigDecimal> expectedDelta = new HashMap<>();
            addTo(expectedDelta, today, "10000");
            addTo(expectedDelta, older.toString(), "20000");
            addTo(expectedDelta, LocalDate.now().minusDays(10).toString(), "30000");
            boolean monthsOk = true;
            StringBuilder detail = new StringBuilder();
            for (Map.Entry<String, BigDecimal> e : expectedDelta.entrySet()) {
                BigDecimal was = monthBefore.containsKey(e.getKey()) ? monthBefore.get(e.getKey()) : BigDecimal.ZERO;
                BigDecimal now = monthAfter.containsKey(e.getKey()) ? monthAfter.get(e.getKey()) : BigDecimal.ZERO;
                boolean same = now.subtract(was).compareTo(e.getValue()) == 0;
                monthsOk &= same;
                detail.append(e.getKey()).append('=').append(now.subtract(was)).append(' ');
            }
            check("expenseByMonth grows by the expected total per month " + detail, monthsOk, true);
            check("expenseByMonth is ordered by month", orderedMonths(DAO.expenseByMonth(5)), true);
            check("expenseByMonth month key format", DAO.expenseByMonth(5).get(0).get("month").toString().matches("\\d{4}-\\d{2}"), true);
            DAO.addExpense(Date.valueOf(LocalDate.now().minusMonths(8)), TAG + "OLD", null, "Other", new BigDecimal("777"), "Cash", null, false, null, actor.getUserId());
            check("expenseByMonth ignores rows older than the window", DAO.expenseByMonth(5).toString().contains("777"), false);
            check("expenseByMonth with a wider window includes them", DAO.expenseByMonth(12).toString().contains(LocalDate.now().minusMonths(8).toString().substring(0, 7)), true);

            c = get("/tax/expenses", new Call().p("q", TAG).p("export", "1"));
            String csv = c.out.toString();
            check("CSV has the header", csv.contains("\"Date\",\"Supplier\",\"Category\",\"Amount\",\"Payment\",\"Invoice\",\"Document\",\"Tax treatment\""), true);
            check("CSV content type", c.contentType, "text/csv;charset=UTF-8");
            check("CSV has a row per expense (header + A, B, C, OLD)", csv.trim().split("\r\n").length, 5);
            check("CSV document column", csv.contains("\"yes\",\"UNCLASSIFIED\"") && csv.contains("\"no\",\"UNCLASSIFIED\""), true);
            post("/tax/expenses", expense("=" + TAG + "F", today, "Other", "5000", "Cash"));
            c = get("/tax/expenses", new Call().p("q", TAG + "F").p("export", "1"));
            check("CSV neutralises a formula in the supplier", c.out.toString().contains("\"'=" + TAG + "F\""), true);

            int a = expenseId(TAG + "A");
            int b = expenseId(TAG + "B");
            c = post("/tax/expenses", new Call().p("action", "treatment").p("id", String.valueOf(b)).p("value", "MAY_COUNT"));
            check("MAY_COUNT without a document blocked", c.redirectedWith("blocked") + "|" + one("SELECT tax_treatment FROM Expenses WHERE expense_id = ?", b), "true|UNCLASSIFIED");
            c = post("/tax/expenses", new Call().p("action", "treatment").p("id", String.valueOf(a)).p("value", "MAY_COUNT"));
            check("MAY_COUNT with a document allowed", c.redirectedWith("saved") + "|" + one("SELECT tax_treatment FROM Expenses WHERE expense_id = ?", a), "true|MAY_COUNT");
            c = post("/tax/expenses", new Call().p("action", "treatment").p("id", String.valueOf(b)).p("value", "NOT_COUNTED"));
            check("NOT_COUNTED needs no document", one("SELECT tax_treatment FROM Expenses WHERE expense_id = ?", b), "NOT_COUNTED");
            c = post("/tax/expenses", new Call().p("action", "treatment").p("id", String.valueOf(a)).p("value", "BOGUS"));
            check("unknown treatment refused", c.redirectedWith("bad") + "|" + one("SELECT tax_treatment FROM Expenses WHERE expense_id = ?", a), "true|MAY_COUNT");
            c = post("/tax/expenses", new Call().p("action", "treatment").p("id", "999999999").p("value", "NOT_COUNTED"));
            check("treatment of a missing expense refused", c.redirectedWith("bad"), true);
            post("/tax/expenses", new Call().p("action", "document").p("id", String.valueOf(b)).p("value", "1"));
            check("document flag turned on", one("SELECT has_document FROM Expenses WHERE expense_id = ?", b), "1");
            c = post("/tax/expenses", new Call().p("action", "treatment").p("id", String.valueOf(b)).p("value", "MAY_COUNT"));
            check("MAY_COUNT allowed after the document is attached", one("SELECT tax_treatment FROM Expenses WHERE expense_id = ?", b), "MAY_COUNT");
            post("/tax/expenses", new Call().p("action", "document").p("id", String.valueOf(b)).p("value", "0"));
            check("document flag turned off", one("SELECT has_document FROM Expenses WHERE expense_id = ?", b), "0");
            check("DB refuses an unknown treatment", dbRejects("UPDATE Expenses SET tax_treatment = 'X' WHERE expense_id = ?", b), true);
            check("DB refuses a zero expense", dbRejects("UPDATE Expenses SET amount = 0 WHERE expense_id = ?", b), true);
            check("setExpenseTreatment on a missing row is false", DAO.setExpenseTreatment(999999999, "NOT_COUNTED"), false);
            check("setExpenseDocument on a missing row is false", DAO.setExpenseDocument(999999999, true), false);

            c = post("/tax/expenses", new Call().p("action", "delete").p("id", String.valueOf(a)));
            check("delete an expense", c.redirectedWith("deleted") + "|" + count("SELECT COUNT(*) FROM Expenses WHERE expense_id = ?", a), "true|0");
            c = post("/tax/expenses", new Call().p("action", "delete").p("id", String.valueOf(a)));
            check("delete again reports missing", c.redirectedWith("missing"), true);
            check("deleteExpense on a missing id is false", DAO.deleteExpense(999999999), false);

            for (int i = 0; i < 12; i++) {
                DAO.addExpense(Date.valueOf(LocalDate.now().minusDays(i)), TAG + "P" + String.format("%02d", i), null, "Other", new BigDecimal("1000"), "Cash", null, false, null, actor.getUserId());
            }
            c = get("/tax/expenses", new Call().p("q", TAG + "P"));
            check("paging: page 1 has 10 rows", items(c).size() + "/" + ((Page<?>) c.attr("result")).getTotal() + "/" + ((Page<?>) c.attr("result")).getTotalPages(), "10/12/2");
            c = get("/tax/expenses", new Call().p("q", TAG + "P").p("page", "2"));
            check("paging: page 2 has 2 rows", items(c).size(), 2);
            check("paging: page 2 continues the order", items(c).get(0).get("supplier"), TAG + "P10");
            c = get("/tax/expenses", new Call().p("q", TAG + "P").p("page", "99"));
            check("paging: a page past the end does not crash", c.forwarded, "expenses.jsp");

            c = get("/tax/obligations", new Call().p("id", String.valueOf(id)));
            check("obligation detail page loads the history and the row", ((List<?>) c.attr("history")).size() + "|" + (c.attr("detail") != null), historyCount(id) + "|true");
            c = get("/tax/obligations", new Call().p("id", "999999999"));
            check("obligation detail of a missing id loads without a row", c.forwarded + "|" + c.attr("detail") + "|" + c.attr("history"), "obligations.jsp|null|null");
            c = get("/tax/payments", new Call());
            check("payments page loads", c.forwarded + "|" + (c.attr("payments") != null), "payments.jsp|true");
        } finally {
            run("DELETE FROM TaxPayments WHERE obligation_id IN (SELECT obligation_id FROM TaxObligations WHERE period_label LIKE ?)", TAG + "%");
            run("DELETE FROM TaxObligationHistory WHERE obligation_id IN (SELECT obligation_id FROM TaxObligations WHERE period_label LIKE ?)", TAG + "%");
            run("DELETE FROM TaxObligations WHERE period_label LIKE ?", TAG + "%");
            run("DELETE FROM Expenses WHERE supplier LIKE ? OR supplier LIKE ?", TAG + "%", "=" + TAG + "%");
            run("DELETE FROM TaxCalculations WHERE calc_id > ? AND (note LIKE ? OR period_label LIKE ? OR note LIKE 'Recalculated from #%')", maxCalc, "%" + TAG + "%", TAG + "%");
            run("DELETE FROM TaxAssessments WHERE assessment_id > ?", maxAssess);
        }
        check("cleanup: obligations", count("SELECT COUNT(*) FROM TaxObligations WHERE period_label LIKE ?", TAG + "%"), 0);
        check("cleanup: expenses", count("SELECT COUNT(*) FROM Expenses WHERE supplier LIKE ? OR supplier LIKE ?", TAG + "%", "=" + TAG + "%"), 0);
        check("cleanup: calculations", count("SELECT COUNT(*) FROM TaxCalculations WHERE calc_id > ?", maxCalc), 0);
        check("cleanup: assessments", count("SELECT COUNT(*) FROM TaxAssessments WHERE assessment_id > ?", maxAssess), 0);
        System.out.println(passed + " checks passed");
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static int countMine(List<Map<String, Object>> rows, int obligationId) {
        int n = 0;
        for (Map<String, Object> r : rows) {
            if (((Integer) r.get("obligation_id")) == obligationId) {
                n++;
            }
        }
        return n;
    }

    private static int countCalc(List<Map<String, Object>> rows, int calcId) {
        int n = 0;
        for (Map<String, Object> r : rows) {
            if (((Integer) r.get("calc_id")) == calcId) {
                n++;
            }
        }
        return n;
    }

    private static String paidOf(int id) throws Exception {
        for (Map<String, Object> r : DAO.obligations()) {
            if (((Integer) r.get("obligation_id")) == id) {
                return ((BigDecimal) r.get("paid_amount")).setScale(0).toPlainString();
            }
        }
        return null;
    }

    private static String lateOf(int id) throws Exception {
        for (Map<String, Object> r : DAO.obligations()) {
            if (((Integer) r.get("obligation_id")) == id) {
                return String.valueOf(r.get("late"));
            }
        }
        return null;
    }

    private static Map<String, BigDecimal> monthTotals() throws Exception {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Map<String, Object> r : DAO.expenseByMonth(5)) {
            map.put((String) r.get("month"), (BigDecimal) r.get("total"));
        }
        return map;
    }

    private static void addTo(Map<String, BigDecimal> map, String date, String amount) {
        String month = date.substring(0, 7);
        BigDecimal old = map.containsKey(month) ? map.get(month) : BigDecimal.ZERO;
        map.put(month, old.add(new BigDecimal(amount)));
    }

    private static boolean orderedMonths(List<Map<String, Object>> rows) {
        for (int i = 1; i < rows.size(); i++) {
            if (((String) rows.get(i - 1).get("month")).compareTo((String) rows.get(i).get("month")) > 0) {
                return false;
            }
        }
        return true;
    }
}
