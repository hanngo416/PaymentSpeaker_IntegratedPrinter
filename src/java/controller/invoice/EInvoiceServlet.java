package controller.invoice;

import controller.auth.LoginServlet;
import dao.ChannelSaleDAO;
import dao.InvoiceDAO;
import dao.OrderDAO;
import dao.SettingDAO;
import dao.SummaryDAO;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.ChannelSale;
import model.Invoice;
import model.Order;
import model.Page;
import model.User;
import model.enums.OrderStatus;
import service.AuditService;
import service.InvoiceService;

@WebServlet(name = "EInvoiceServlet", urlPatterns = {"/invoices/manage", "/invoices/new", "/invoices/export"})
public class EInvoiceServlet extends HttpServlet {

    private static final String LIST_VIEW = "/WEB-INF/views/invoice/invoices.jsp";
    private static final String FORM_VIEW = "/WEB-INF/views/invoice/invoice-form.jsp";
    private static final int PAGE_SIZE = 10;
    private static final long THRESHOLD = 1000000000L;
    private static final Pattern ORDER_CODE = Pattern.compile("TINKY\\d{5}");
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("created", "Draft invoice created. It is not valid until your e-invoice provider issues it.");
        MESSAGES.put("updated", "Invoice saved.");
        MESSAGES.put("sent", "Invoice marked as sent to the provider.");
        MESSAGES.put("issued", "Invoice marked as issued.");
        MESSAGES.put("deleted", "Draft invoice deleted.");
        MESSAGES.put("seller", "Seller details saved.");
        MESSAGES.put("dup", "An invoice already exists for this order or sale.");
        MESSAGES.put("notfound", "Order or sale not found.");
        MESSAGES.put("notcompleted", "Only a completed order or sale can have an invoice.");
        MESSAGES.put("notdraft", "Only a draft invoice can be changed or deleted.");
        MESSAGES.put("badprovider", "Enter the provider invoice number (up to 30) and lookup code (up to 40) using letters, digits and / _ . - only.");
        MESSAGES.put("badstate", "The invoice status has changed. Nothing was done.");
        MESSAGES.put("badorder", "Enter an order code like TINKY12345.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final InvoiceService service = new InvoiceService();
    private final InvoiceDAO dao = new InvoiceDAO();
    private final SettingDAO settingDAO = new SettingDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        try {
            if ("/invoices/export".equals(path)) {
                export(request, response);
            } else if ("/invoices/new".equals(path)) {
                showForm(request, response);
            } else {
                request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
                showList(request, response, null);
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load e-invoices", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String path = request.getServletPath();
        String action = trim(request.getParameter("action"));
        try {
            if ("/invoices/new".equals(path)) {
                if ("create".equals(action)) {
                    create(request, response, actor);
                } else if ("update".equals(action)) {
                    update(request, response, actor);
                } else {
                    redirect(request, response, "bad");
                }
            } else if ("/invoices/manage".equals(path)) {
                if ("seller".equals(action)) {
                    saveSeller(request, response, actor);
                } else if ("order".equals(action)) {
                    findOrder(request, response);
                } else if ("sent".equals(action)) {
                    markSent(request, response, actor);
                } else if ("issue".equals(action)) {
                    markIssued(request, response, actor);
                } else if ("delete".equals(action)) {
                    delete(request, response, actor);
                } else {
                    redirect(request, response, "bad");
                }
            } else {
                redirect(request, response, "bad");
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot update e-invoices", e);
        }
    }

    private void showList(HttpServletRequest request, HttpServletResponse response, Map<String, String> sellerErrors)
            throws SQLException, ServletException, IOException {
        String status = trim(request.getParameter("status"));
        if (!validStatus(status)) {
            status = "";
        }
        int pageNo = clampPage(request.getParameter("page"));
        Page<Invoice> data = dao.search(status.isEmpty() ? null : status, pageNo, PAGE_SIZE);
        if (data.getItems().isEmpty() && data.getTotal() > 0 && pageNo > data.getTotalPages()) {
            data = dao.search(status.isEmpty() ? null : status, data.getTotalPages(), PAGE_SIZE);
        }
        int year = LocalDate.now().getYear();
        Map<String, String> seller = new HashMap<>();
        if (sellerErrors != null) {
            seller.put("name", trim(request.getParameter("sellerName")));
            seller.put("tax", trim(request.getParameter("sellerTax")));
            seller.put("address", trim(request.getParameter("sellerAddress")));
        } else {
            seller.put("name", settingDAO.get(InvoiceService.SELLER_NAME, "My Store"));
            seller.put("tax", settingDAO.get(InvoiceService.SELLER_TAX_CODE, ""));
            seller.put("address", settingDAO.get(InvoiceService.SELLER_ADDRESS, ""));
        }
        request.setAttribute("invoices", data);
        request.setAttribute("status", status);
        request.setAttribute("seller", seller);
        request.setAttribute("sellerErrors", sellerErrors == null ? new HashMap<String, String>() : sellerErrors);
        request.setAttribute("year", year);
        request.setAttribute("yearRevenue", new SummaryDAO().yearRevenue(year));
        request.setAttribute("threshold", BigDecimal.valueOf(THRESHOLD));
        request.getRequestDispatcher(LIST_VIEW).forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Map<String, String> f = new HashMap<>();
        Integer invoiceId = parseId(request.getParameter("invoiceId"));
        if (invoiceId != null) {
            Invoice inv = dao.find(invoiceId);
            if (inv == null) {
                redirect(request, response, "notfound");
                return;
            }
            if (!"DRAFT".equals(inv.getStatus())) {
                redirect(request, response, "notdraft");
                return;
            }
            f.put("name", inv.getBuyerName());
            f.put("tax", inv.getBuyerTaxCode() == null ? "" : inv.getBuyerTaxCode());
            f.put("address", inv.getBuyerAddress() == null ? "" : inv.getBuyerAddress());
            request.setAttribute("invoice", inv);
            request.setAttribute("mode", "edit");
        } else {
            String source = trim(request.getParameter("source"));
            Integer sourceId = parseId(request.getParameter("id"));
            String problem = sourceProblem(source, sourceId, request);
            if (problem != null) {
                redirect(request, response, problem);
                return;
            }
            f.put("name", InvoiceService.DEFAULT_BUYER);
            f.put("tax", "");
            f.put("address", "");
            request.setAttribute("mode", "create");
            request.setAttribute("source", source);
            request.setAttribute("sourceId", sourceId);
        }
        renderForm(request, response, f, new HashMap<String, String>());
    }

    private void renderForm(HttpServletRequest request, HttpServletResponse response, Map<String, String> f,
            Map<String, String> errors) throws ServletException, IOException {
        request.setAttribute("f", f);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(FORM_VIEW).forward(request, response);
    }

    private String sourceProblem(String source, Integer sourceId, HttpServletRequest request) throws SQLException {
        if (sourceId == null || (!"ORDER".equals(source) && !"CHANNEL_SALE".equals(source))) {
            return "bad";
        }
        if ("ORDER".equals(source)) {
            Order o = dao.findOrder(sourceId);
            if (o == null) {
                return "notfound";
            }
            if (o.getStatus() != OrderStatus.COMPLETED) {
                return "notcompleted";
            }
            request.setAttribute("sourceLabel", "Counter order " + o.getOrderCode());
        } else {
            ChannelSale s = new ChannelSaleDAO().find(sourceId);
            if (s == null) {
                return "notfound";
            }
            if (!"COMPLETED".equals(s.getStatus())) {
                return "notcompleted";
            }
            request.setAttribute("sourceLabel", "Online order " + s.getExternalRef() + " (" + s.getChannel() + ")");
        }
        if (dao.findIdBySource(source, sourceId) != null) {
            return "dup";
        }
        return null;
    }

    private void create(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        String source = trim(request.getParameter("source"));
        Integer sourceId = parseId(request.getParameter("sourceId"));
        String problem = sourceProblem(source, sourceId, request);
        if (problem != null) {
            redirect(request, response, problem);
            return;
        }
        Map<String, String> f = buyerFields(request);
        Map<String, String> errors = validateBuyer(f);
        if (!errors.isEmpty()) {
            request.setAttribute("mode", "create");
            request.setAttribute("source", source);
            request.setAttribute("sourceId", sourceId);
            renderForm(request, response, f, errors);
            return;
        }
        try {
            Invoice inv;
            if ("ORDER".equals(source)) {
                inv = service.draftFromOrder(sourceId, actor.getUserId(), f.get("name"), nullIfEmpty(f.get("tax")),
                        nullIfEmpty(f.get("address")));
            } else {
                inv = service.draftFromChannelSale(sourceId, actor.getUserId(), f.get("name"),
                        nullIfEmpty(f.get("tax")), nullIfEmpty(f.get("address")));
            }
            AuditService.log(actor, "EINVOICE_DRAFT", "INVOICE", inv.getInvoiceNo(), "source", source + ":" + sourceId);
            redirect(request, response, "created");
        } catch (InvoiceService.Refused e) {
            redirect(request, response, e.getCode());
        }
    }

    private void update(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        Integer id = parseId(request.getParameter("invoiceId"));
        Invoice inv = id == null ? null : dao.find(id);
        if (inv == null) {
            redirect(request, response, "notfound");
            return;
        }
        if (!"DRAFT".equals(inv.getStatus())) {
            redirect(request, response, "notdraft");
            return;
        }
        Map<String, String> f = buyerFields(request);
        Map<String, String> errors = validateBuyer(f);
        if (!errors.isEmpty()) {
            request.setAttribute("mode", "edit");
            request.setAttribute("invoice", inv);
            renderForm(request, response, f, errors);
            return;
        }
        if (service.updateBuyer(id, f.get("name"), nullIfEmpty(f.get("tax")), nullIfEmpty(f.get("address")))) {
            AuditService.log(actor, "EINVOICE_DRAFT", "INVOICE", inv.getInvoiceNo(), "change", "buyer");
            redirect(request, response, "updated");
        } else {
            redirect(request, response, "notdraft");
        }
    }

    private void saveSeller(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, ServletException, IOException {
        String name = trim(request.getParameter("sellerName"));
        String tax = trim(request.getParameter("sellerTax"));
        String address = trim(request.getParameter("sellerAddress"));
        Map<String, String> errors = new HashMap<>();
        if (name.length() < 1 || name.length() > 100) {
            errors.put("name", "1-100 characters.");
        }
        if (!tax.isEmpty() && !InvoiceService.validTaxCode(tax)) {
            errors.put("tax", "10 digits, or 10 digits, a dash and 3 digits. Leave empty if you have none.");
        }
        if (address.length() > 200) {
            errors.put("address", "At most 200 characters.");
        }
        if (!errors.isEmpty()) {
            showList(request, response, errors);
            return;
        }
        settingDAO.set(InvoiceService.SELLER_NAME, name);
        settingDAO.set(InvoiceService.SELLER_TAX_CODE, tax);
        settingDAO.set(InvoiceService.SELLER_ADDRESS, address);
        AuditService.log(actor, "EINVOICE_SELLER", "SETTINGS", "seller", "name", name);
        redirect(request, response, "seller");
    }

    private void findOrder(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        String code = trim(request.getParameter("orderCode")).toUpperCase();
        if (!ORDER_CODE.matcher(code).matches()) {
            redirect(request, response, "badorder");
            return;
        }
        Order o = new OrderDAO().findByCode(code);
        if (o == null) {
            redirect(request, response, "notfound");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/invoices/new?source=ORDER&id=" + o.getOrderId());
    }

    private void markSent(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, IOException {
        Integer id = parseId(request.getParameter("id"));
        if (id == null) {
            redirect(request, response, "bad");
            return;
        }
        if (service.markSent(id)) {
            AuditService.log(actor, "EINVOICE_SENT", "INVOICE", id);
            redirect(request, response, "sent");
        } else {
            redirect(request, response, "badstate");
        }
    }

    private void markIssued(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, IOException {
        Integer id = parseId(request.getParameter("id"));
        if (id == null) {
            redirect(request, response, "bad");
            return;
        }
        String no = trim(request.getParameter("providerNo"));
        String lookup = trim(request.getParameter("lookupCode"));
        if (!InvoiceService.validProviderNo(no) || !InvoiceService.validLookupCode(lookup)) {
            redirect(request, response, "badprovider");
            return;
        }
        if (service.markIssued(id, no, lookup)) {
            AuditService.log(actor, "EINVOICE_ISSUED", "INVOICE", id, "provider_invoice_no", no);
            redirect(request, response, "issued");
        } else {
            redirect(request, response, "badstate");
        }
    }

    private void delete(HttpServletRequest request, HttpServletResponse response, User actor)
            throws SQLException, IOException {
        Integer id = parseId(request.getParameter("id"));
        if (id == null) {
            redirect(request, response, "bad");
            return;
        }
        if (service.delete(id)) {
            AuditService.log(actor, "EINVOICE_DELETE", "INVOICE", id);
            redirect(request, response, "deleted");
        } else {
            redirect(request, response, "notdraft");
        }
    }

    private void export(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        Integer id = parseId(request.getParameter("id"));
        Invoice inv = id == null ? null : dao.find(id);
        if (inv == null) {
            redirect(request, response, "notfound");
            return;
        }
        String csv = service.exportCsv(id);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + inv.getInvoiceNo() + ".csv\"");
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        response.setContentLength(body.length + 3);
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        response.getOutputStream().write(body);
    }

    private static Map<String, String> buyerFields(HttpServletRequest request) {
        Map<String, String> f = new HashMap<>();
        f.put("name", trim(request.getParameter("buyerName")));
        f.put("tax", trim(request.getParameter("buyerTax")));
        f.put("address", trim(request.getParameter("buyerAddress")));
        return f;
    }

    static Map<String, String> validateBuyer(Map<String, String> f) {
        Map<String, String> errors = new HashMap<>();
        if (f.get("name").length() < 1 || f.get("name").length() > 100) {
            errors.put("name", "1-100 characters.");
        }
        if (!f.get("tax").isEmpty() && !InvoiceService.validTaxCode(f.get("tax"))) {
            errors.put("tax", "10 digits, or 10 digits, a dash and 3 digits. Leave empty if the buyer has none.");
        }
        if (f.get("address").length() > 200) {
            errors.put("address", "At most 200 characters.");
        }
        return errors;
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String msg) throws IOException {
        response.sendRedirect(request.getContextPath() + "/invoices/manage?msg=" + msg);
    }

    private static boolean validStatus(String s) {
        return "DRAFT".equals(s) || "SENT_TO_PROVIDER".equals(s) || "ISSUED".equals(s);
    }

    private static int clampPage(String raw) {
        try {
            return Math.max(1, Math.min(Integer.parseInt(trim(raw)), 100000));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Integer parseId(String raw) {
        try {
            int v = Integer.parseInt(trim(raw));
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String nullIfEmpty(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
