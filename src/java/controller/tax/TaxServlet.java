package controller.tax;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.TaxService;

@WebServlet(name = "TaxServlet", urlPatterns = {"/tax"})
public class TaxServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/tax/tax.jsp";

    private final TaxService taxService = new TaxService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int now = LocalDate.now().getYear();
        int year = now;
        String raw = request.getParameter("year");
        if (raw != null && raw.matches("20\\d{2}")) {
            year = Integer.parseInt(raw);
        }
        try {
            request.setAttribute("tax", taxService.view(year));
        } catch (SQLException e) {
            throw new ServletException("Cannot build the tax estimate", e);
        }
        request.setAttribute("year", year);
        request.setAttribute("thisYear", now);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
