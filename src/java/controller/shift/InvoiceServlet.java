package controller.shift;

import controller.auth.LoginServlet;
import dao.DeviceDAO;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import model.view.Report;
import model.view.Summary;
import service.AuditService;
import service.ReportService;

/**
 * Statistics by day, month or year for admins and auditors: on screen, as a printout or as a CSV file.
 */
@WebServlet(name = "InvoiceServlet", urlPatterns = {"/invoices"})
public class InvoiceServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/shift/invoices.jsp";
    private static final String PRINT = "/WEB-INF/views/shift/report-print.jsp";

    private final ReportService reportService = new ReportService();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String type = request.getParameter("type");
        if (!"month".equals(type) && !"year".equals(type)) {
            type = "day";
        }
        String error = null;
        Report report = null;
        String day = valueOr(request.getParameter("day"), LocalDate.now().toString());
        String month = valueOr(request.getParameter("month"), YearMonth.now().toString());
        String year = valueOr(request.getParameter("year"), String.valueOf(LocalDate.now().getYear()));
        try {
            if ("day".equals(type)) {
                report = reportService.forDay(LocalDate.parse(day));
            } else if ("month".equals(type)) {
                report = reportService.forMonth(YearMonth.parse(month));
            } else {
                int y = Integer.parseInt(year);
                if (y < 2000 || y > 2100) {
                    throw new NumberFormatException();
                }
                report = reportService.forYear(y);
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            error = "Choose a valid " + type + ".";
        } catch (SQLException e) {
            throw new ServletException("Cannot build the report", e);
        }

        String format = request.getParameter("format");
        if (report != null && "csv".equals(format)) {
            AuditService.log(actor, "EXPORT_INVOICES", "REPORT", report.getPeriod(), "type", type);
            writeCsv(response, report);
            return;
        }
        if (report != null && "print".equals(format)) {
            try {
                if (deviceDAO.isInMaintenance("PRINTER")) {
                    request.setAttribute("message", "The printer is in maintenance, nothing can be printed now.");
                    report = null;
                }
            } catch (SQLException e) {
                throw new ServletException("Cannot check the printer", e);
            }
            if (report != null) {
                AuditService.log(actor, "PRINT_REPORT", "REPORT", report.getPeriod(), "type", type);
                request.setAttribute("report", report);
                request.setAttribute("printedBy", actor.getUserCode());
                request.setAttribute("printedAt", new Date());
                request.getRequestDispatcher(PRINT).forward(request, response);
                return;
            }
        }
        if (request.getAttribute("message") == null) {
            request.setAttribute("message", "busy".equals(request.getParameter("msg"))
                    ? "The printer is in maintenance, nothing can be printed now."
                    : "missing".equals(request.getParameter("msg")) ? "That shift was not found." : null);
        }
        request.setAttribute("recent", recentShifts());
        request.setAttribute("type", type);
        request.setAttribute("day", day);
        request.setAttribute("month", month);
        request.setAttribute("year", year);
        request.setAttribute("report", report);
        request.setAttribute("error", error);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private java.util.List<model.view.ShiftRow> recentShifts() throws ServletException {
        try {
            return reportService.recentShifts(10);
        } catch (SQLException e) {
            throw new ServletException("Cannot load the shifts", e);
        }
    }

    private static void writeCsv(HttpServletResponse response, Report report) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"invoices-" + report.getPeriod() + ".csv\"");
        PrintWriter out = response.getWriter();
        out.print('﻿');   // lets Excel read the file as UTF-8
        out.print("Period,Paid orders,Order total,Cash,Expected by transfer,Received in account,Difference\r\n");
        if (report.getRows().isEmpty()) {
            csvLine(out, report.getPeriod(), report.getSummary());
        } else {
            for (Summary s : report.getRows()) {
                csvLine(out, s.getLabel(), s);
            }
            csvLine(out, "TOTAL", report.getSummary());
        }
    }

    private static void csvLine(PrintWriter out, String label, Summary s) {
        out.print(label + "," + s.getOrderCount() + "," + s.getTotal().toPlainString() + "," + s.getCash().toPlainString()
                + "," + s.getTransfer().toPlainString() + "," + s.getBankReceived().toPlainString()
                + "," + s.getBankDiff().toPlainString() + "\r\n");
    }

    private static String valueOr(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
