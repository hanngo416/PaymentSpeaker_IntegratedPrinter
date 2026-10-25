/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller.tax;

import controller.affiliate.AffiliateBase;
import dao.TaxDocumentDAO;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.User;
import service.AuditService;
import static util.Params.parseInt;

/**
 *
 * @author PC
 */
@WebServlet(name = "TaxDocumentServlet", urlPatterns = {"/tax/documents"})
@MultipartConfig(maxFileSize = 10485760L, maxRequestSize = 11534336L)
public class TaxDocumentServlet extends AffiliateBase {

    private static final int PAGE_SIZE = 10;
    private static final String[] CATEGORIES = {"REGISTRATION", "TAX_CODE", "INVOICE", "BANK", "EXPENSE", "FILING", "OTHER"};
    private static final String[] STATUSES = {"UPLOADED", "MISSING", "REVIEW"};
    private static final Map<String, String> TYPES = new HashMap<>();
    private static final Map<String, String> TEXT = new HashMap<>();

    static {
        TYPES.put("pdf", "application/pdf");
        TYPES.put("png", "image/png");
        TYPES.put("jpg", "image/jpeg");
        TYPES.put("jpeg", "image/jpeg");
        TYPES.put("csv", "text/csv");
        TYPES.put("xls", "application/vnd.ms-excel");
        TYPES.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        TYPES.put("doc", "application/msword");
        TYPES.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        TEXT.put("saved", "Đã lưu tài liệu.");
        TEXT.put("updated", "Đã cập nhật trạng thái.");
        TEXT.put("deleted", "Đã xóa tài liệu.");
        TEXT.put("nofile", "Hãy chọn một tệp để tải lên.");
        TEXT.put("badtype", "Chỉ nhận tệp PDF, PNG, JPG, CSV, XLS, XLSX, DOC, DOCX.");
        TEXT.put("toobig", "Tệp vượt quá 10 MB.");
        TEXT.put("badname", "Tên tài liệu phải từ 2 đến 150 ký tự.");
        TEXT.put("badcat", "Nhóm hoặc kỳ không hợp lệ.");
        TEXT.put("missing", "Không tìm thấy tài liệu hoặc trạng thái không cho phép thao tác này.");
    }

    private final TaxDocumentDAO dao = new TaxDocumentDAO();

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
        try {
            Integer download = parseInt(request.getParameter("download"));
            if (download != null) {
                sendFile(response, download);
                return;
            }
            String q = trim(request.getParameter("q"));
            String category = oneOf(trim(request.getParameter("category")), CATEGORIES);
            String status = oneOf(trim(request.getParameter("status")), STATUSES);
            List<Map<String, Object>> all = dao.search(q.length() > 100 ? q.substring(0, 100) : q, category, status);
            int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            int page = Math.min(Math.max(intParam(request, "page", 1), 1), pages);
            request.setAttribute("docs", all.subList((page - 1) * PAGE_SIZE, Math.min(all.size(), page * PAGE_SIZE)));
            request.setAttribute("page", page);
            request.setAttribute("pages", pages);
            request.setAttribute("q", q);
            request.setAttribute("category", category);
            request.setAttribute("status", status);
            request.setAttribute("counts", dao.counts());
            request.setAttribute("message", TEXT.get(request.getParameter("msg")));
            request.setAttribute("canManage", filter.RolePermissions.has(user(request).getRole(), model.enums.Permission.TAX_MANAGE));
            request.getRequestDispatcher("/WEB-INF/views/tax/documents.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load the documents", e);
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
        User actor = user(request);
        String action = trim(request.getParameter("action"));
        try {
            String msg;
            switch (action) {
                case "upload":
                    msg = upload(request, actor, false);
                    break;
                case "attach":
                    msg = upload(request, actor, true);
                    break;
                case "review":
                case "unreview": {
                    Integer id = parseInt(request.getParameter("id"));
                    boolean ok = id != null && dao.setReview(id, "review".equals(action));
                    if (ok) {
                        AuditService.log(actor, "TAX_DOC_STATUS", "TAX", id, action);
                    }
                    msg = ok ? "updated" : "missing";
                    break;
                }
                case "delete": {
                    Integer id = parseInt(request.getParameter("id"));
                    boolean ok = id != null && dao.delete(id);
                    if (ok) {
                        AuditService.log(actor, "TAX_DOC_DELETE", "TAX", id);
                    }
                    msg = ok ? "deleted" : "missing";
                    break;
                }
                default:
                    msg = "missing";
            }
            redirect(request, response, "/tax/documents", msg);
        } catch (SQLException e) {
            throw new ServletException("Cannot save the document", e);
        }
    }

    private String upload(HttpServletRequest request, User actor, boolean attach) throws IOException, ServletException, SQLException {
        Part part = request.getPart("file");
        if (part == null || part.getSize() == 0) {
            return "nofile";
        }
        if (part.getSize() > 10485760L) {
            return "toobig";
        }
        String fileName = cleanName(part.getSubmittedFileName());
        String ext = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        String type = TYPES.get(ext);
        if (type == null) {
            return "badtype";
        }
        int size = (int) part.getSize();
        try (InputStream in = part.getInputStream()) {
            if (attach) {
                Integer id = parseInt(request.getParameter("id"));
                boolean ok = id != null && dao.attach(id, fileName, type, size, in, actor.getUserId());
                if (ok) {
                    AuditService.log(actor, "TAX_DOC_UPLOAD", "TAX", id, fileName);
                }
                return ok ? "saved" : "missing";
            }
            String name = trim(request.getParameter("name"));
            if (name.isEmpty()) {
                name = fileName;
            }
            String category = oneOf(trim(request.getParameter("category")), CATEGORIES);
            String period = trim(request.getParameter("period"));
            if (name.length() < 2 || name.length() > 150) {
                return "badname";
            }
            if (category.isEmpty() || period.length() > 40) {
                return "badcat";
            }
            int id = dao.insert(name, category, period, fileName, type, size, in, actor.getUserId());
            AuditService.log(actor, "TAX_DOC_UPLOAD", "TAX", id, fileName);
            return "saved";
        }
    }

    private void sendFile(HttpServletResponse response, int id) throws SQLException, IOException {
        Object[] file = dao.file(id);
        if (file == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        byte[] data = (byte[]) file[2];
        response.setContentType(String.valueOf(file[1]));
        response.setHeader("Content-Disposition", "attachment; filename=\"" + String.valueOf(file[0]).replaceAll("[^A-Za-z0-9._-]", "_") + "\"");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    private static String cleanName(String raw) {
        String name = raw == null ? "" : raw;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1).trim();
        return name.length() > 150 ? name.substring(name.length() - 150) : name;
    }
}
