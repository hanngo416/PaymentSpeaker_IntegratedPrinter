package controller.printer;

import controller.auth.LoginServlet;
import dao.DeviceDAO;
import dao.TransactionDAO;
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

/**
 * Receipt printer: /printer lists the money received, /printer/print opens the receipt of one transfer and marks it printed.
 */
@WebServlet(name = "PrinterServlet", urlPatterns = {"/printer", "/printer/print"})
public class PrinterServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/printer/printer.jsp";
    private static final String RECEIPT = "/WEB-INF/views/printer/receipt.jsp";
    private static final int LIST_SIZE = 30;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/printer".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        try {
            deviceDAO.ping("PRINTER");
            request.setAttribute("maintenance", deviceDAO.isInMaintenance("PRINTER"));
            request.setAttribute("receipts", transactionDAO.findPrintable(LIST_SIZE, cashierScope(actor)));
        } catch (SQLException e) {
            throw new ServletException("Cannot load the printer page", e);
        }
        request.setAttribute("message", "busy".equals(request.getParameter("msg"))
                ? "The printer is in maintenance, receipts cannot be printed now." : null);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/printer/print".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            if (deviceDAO.isInMaintenance("PRINTER")) {
                response.sendRedirect(request.getContextPath() + "/printer?msg=busy");
                return;
            }
            ReceiptRow receipt = transactionDAO.findReceipt(id, cashierScope(actor));
            if (receipt == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean reprint = receipt.isPrinted();
            transactionDAO.markPrinted(id);
            AuditService.log(actor, reprint ? "REPRINT_RECEIPT" : "PRINT_RECEIPT", "TRANSACTION", id,
                    "order", receipt.getOrderCode());
            request.setAttribute("receipt", receipt);
            request.setAttribute("reprint", reprint);
            request.getRequestDispatcher(RECEIPT).forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Cannot print the receipt", e);
        }
    }

    // A cashier only prints receipts of the orders they created
    private static Integer cashierScope(User actor) {
        return actor.getRole() == Role.CASHIER ? actor.getUserId() : null;
    }
}
