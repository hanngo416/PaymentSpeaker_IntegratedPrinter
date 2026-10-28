package controller.sales;

import controller.auth.LoginServlet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import model.User;
import service.AuditService;
import service.SalesImportService;

@WebServlet(name = "SalesImportServlet", urlPatterns = {"/sales/import", "/sales/import/template"})
@MultipartConfig(maxFileSize = 2 * 1024 * 1024)
public class SalesImportServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/sales/import.jsp";
    private static final int MAX_SHOWN = 50;
    private static final String SHOPEE_TEMPLATE
            = "M\u00e3 \u0111\u01a1n h\u00e0ng,Ng\u00e0y \u0111\u1eb7t h\u00e0ng,Tr\u1ea1ng th\u00e1i \u0111\u01a1n h\u00e0ng,M\u00e3 SKU,T\u00ean s\u1ea3n ph\u1ea9m,T\u00ean ph\u00e2n lo\u1ea1i h\u00e0ng,S\u1ed1 l\u01b0\u1ee3ng,T\u1ed5ng gi\u00e1 b\u00e1n s\u1ea3n ph\u1ea9m,M\u00e3 gi\u1ea3m gi\u00e1 c\u1ee7a Shop,M\u00e3 gi\u1ea3m gi\u00e1 c\u1ee7a Shopee,Ph\u00ed d\u1ecbch v\u1ee5,Ph\u00ed x\u1eed l\u00fd giao d\u1ecbch,Ph\u00ed v\u1eadn chuy\u1ec3n ng\u01b0\u1eddi b\u00e1n tr\u1ea3,Ph\u00ed ti\u1ebfp th\u1ecb li\u00ean k\u1ebft,S\u1ed1 ti\u1ec1n th\u1ef1c nh\u1eadn,K\u00eanh b\u00e1n h\u00e0ng,Gi\u00e1 v\u1ed1n\r\n"
            + "261031SP001,2026-10-31 09:30:00,Ho\u00e0n th\u00e0nh,CF-DEN-01,C\u00e0 ph\u00ea \u0111en Tinky,Ly l\u1edbn,2,70000,5000,10000,5600,2100,0,3500,53800,LIVE,28000\r\n"
            + "261031SP002,2026-10-31 10:15:00,\u0110\u00e3 giao,CF-SUA-02,C\u00e0 ph\u00ea s\u1eefa \u0111\u00e1 Tinky,Ly v\u1eeba,3,105000,0,15000,8400,3150,10000,5250,78200,Video,45000\r\n";
    private static final String TIKTOK_TEMPLATE
            = "Order ID,Created Time,Order Status,Seller SKU,Product Name,Variation,Quantity,Subtotal after seller discounts,Seller discounts,TikTok discounts,TikTok Shop commission fee,Transaction fee,Seller Shipping Fee,Creator Commission,Total settlement amount,Traffic Type,Cost of Goods\r\n"
            + "TT261031TK01,2026-10-31 08:45:00,Completed,CF-DEN-01,C\u00e0 ph\u00ea \u0111en Tinky,Ly l\u1edbn,2,70000,-5000,-10000,-4200,-2100,0,-3500,55200,LIVE,28000\r\n"
            + "TT261031TK02,2026-10-31 09:20:00,Delivered,CF-SUA-02,C\u00e0 ph\u00ea s\u1eefa \u0111\u00e1 Tinky,Ly v\u1eeba,3,105000,0,-15000,-6300,-3150,-12000,-5250,78300,Video,45000\r\n";

    private final SalesImportService service = new SalesImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getServletPath().endsWith("/template")) {
            String ch = request.getParameter("channel");
            boolean isTiktok = "TIKTOK".equalsIgnoreCase(ch);
            String filename = isTiktok ? "tiktok-template.csv" : "shopee-template.csv";
            String content = isTiktok ? TIKTOK_TEMPLATE : SHOPEE_TEMPLATE;
            response.setContentType("text/csv; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.getOutputStream().write(content.getBytes(StandardCharsets.UTF_8));
            return;
        }
        show(request, response, "SHOPEE");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        Part file;
        try {
            file = request.getPart("file");
        } catch (IllegalStateException e) {
            request.setAttribute("formError", "The file is larger than 2 MB.");
            show(request, response, "SHOPEE");
            return;
        } catch (ServletException e) {
            request.setAttribute("formError", "Choose a CSV file to import.");
            show(request, response, "SHOPEE");
            return;
        }
        String channel = request.getParameter("channel") == null ? "" : request.getParameter("channel").trim();
        if (!SalesServlet.validChannel(channel)) {
            request.setAttribute("formError", "Choose Shopee or TikTok Shop.");
            show(request, response, "SHOPEE");
            return;
        }
        if (file == null || file.getSize() == 0) {
            request.setAttribute("formError", "Choose a CSV file to import.");
            show(request, response, channel);
            return;
        }
        try (InputStream in = file.getInputStream()) {
            SalesImportService.Result r = service.importCsv(in, channel, actor.getUserId());
            AuditService.log(actor, "SALES_IMPORT", "CHANNEL_SALE", channel,
                    "imported", String.valueOf(r.imported), "duplicates", String.valueOf(r.duplicates),
                    "errors", String.valueOf(r.errors.size()));
            List<String> shown = new ArrayList<>(r.errors.subList(0, Math.min(MAX_SHOWN, r.errors.size())));
            request.setAttribute("done", Boolean.TRUE);
            request.setAttribute("imported", r.imported);
            request.setAttribute("duplicates", r.duplicates);
            request.setAttribute("errorCount", r.errors.size());
            request.setAttribute("errorLines", shown);
        } catch (SQLException e) {
            throw new ServletException("Cannot import the sales file", e);
        }
        show(request, response, channel);
    }

    private void show(HttpServletRequest request, HttpServletResponse response, String channel)
            throws ServletException, IOException {
        request.setAttribute("channel", channel);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
