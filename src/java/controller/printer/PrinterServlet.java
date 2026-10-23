package controller.printer;

import controller.auth.LoginServlet;
import dao.DeviceDAO;
import dao.OrderItemDAO;
import dao.SettingDAO;
import dao.TransactionDAO;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.enums.Role;
import model.view.ReceiptRow;
import service.AuditService;
import service.PrinterStatusService;
import service.ReceiptPrinter;

@WebServlet(name = "PrinterServlet", urlPatterns = {"/printer/print"})
public class PrinterServlet extends HttpServlet {

    private static final String RECEIPT = "/WEB-INF/views/printer/receipt.jsp";

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final OrderItemDAO orderItemDAO = new OrderItemDAO();
    private final SettingDAO settingDAO = new SettingDAO();
    private final ReceiptPrinter receiptPrinter = new ReceiptPrinter();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        showReceipt(request, response, actor);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/printer/print".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        boolean byOrder = request.getParameter("order") != null;
        int id;
        try {
            id = Integer.parseInt(request.getParameter(byOrder ? "order" : "id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            deviceDAO.ping("PRINTER");
            if (deviceDAO.isInMaintenance("PRINTER")) {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "The printer is in maintenance, receipts cannot be printed now.");
                return;
            }
            ReceiptRow receipt = byOrder ? transactionDAO.findReceiptByOrder(id, cashierScope(actor))
                    : transactionDAO.findReceipt(id, cashierScope(actor));
            if (receipt == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean reprint = receipt.isPrinted();
            boolean sent = false;
            String reason = "";
            try {
                receiptPrinter.print(settingDAO.get(PrinterStatusService.SETTING_KEY, ""),
                        ReceiptPrinter.receipt(receipt, orderItemDAO.findByOrderId(receipt.getOrderId()), reprint));
                sent = true;
            } catch (PrinterException | RuntimeException e) {
                reason = String.valueOf(e.getMessage());
            }
            if (sent) {
                if (receipt.getTransId() > 0) {
                    transactionDAO.markPrinted(receipt.getTransId());
                }
                AuditService.log(actor, reprint ? "REPRINT_RECEIPT" : "PRINT_RECEIPT", byOrder ? "ORDER" : "TRANSACTION", id,
                        "order", receipt.getOrderCode());
            } else {
                AuditService.log(actor, "PRINT_FAILED", byOrder ? "ORDER" : "TRANSACTION", id,
                        "order", receipt.getOrderCode(), "reason", reason);
            }
            if ("1".equals(request.getParameter("silent"))) {
                response.setStatus(sent ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            response.sendRedirect(request.getContextPath() + "/printer/print?" + (byOrder ? "order=" : "id=") + id
                    + (reprint ? "&copy=1" : "") + "&sent=" + (sent ? "1" : "0"));
        } catch (SQLException e) {
            throw new ServletException("Cannot print the receipt", e);
        }
    }

    private void showReceipt(HttpServletRequest request, HttpServletResponse response, User actor)
            throws ServletException, IOException {
        boolean byOrder = request.getParameter("order") != null;
        int id;
        try {
            id = Integer.parseInt(request.getParameter(byOrder ? "order" : "id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            ReceiptRow receipt = byOrder ? transactionDAO.findReceiptByOrder(id, cashierScope(actor))
                    : transactionDAO.findReceipt(id, cashierScope(actor));
            if (receipt == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("receipt", receipt);
            request.setAttribute("orderItems", orderItemDAO.findByOrderId(receipt.getOrderId()));
            request.setAttribute("reprint", "1".equals(request.getParameter("copy")));
            request.getRequestDispatcher(RECEIPT).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot load the receipt", e);
        }
    }

    private static Integer cashierScope(User actor) {
        return actor.getRole() == Role.CASHIER ? actor.getUserId() : null;
    }
}
