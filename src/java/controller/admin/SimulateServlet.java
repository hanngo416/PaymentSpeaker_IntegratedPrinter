package controller.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import service.TransactionIngestService;

@WebServlet(name = "SimulateServlet", urlPatterns = {"/admin/simulate"})
public class SimulateServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/simulate.jsp";

    private final TransactionIngestService ingestService = new TransactionIngestService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        StringBuilder log = new StringBuilder();
        try {
            if ("batch".equals(action)) {
                log.append(send("70000", "Coffee payment")).append('\n');
                log.append(send("70000", "Coffee payment")).append('\n');
                log.append(send("25000", "Unknown transfer")).append('\n');
            } else {
                log.append(send(request.getParameter("amount"), request.getParameter("content")));
            }
        } catch (IllegalArgumentException e) {
            request.setAttribute("error", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Cannot ingest the simulated transaction", e);
        }
        request.setAttribute("result", log.toString());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private String send(String amount, String content) throws SQLException {
        Map<String, String> payload = new HashMap<>();
        payload.put("id", "SIM" + System.nanoTime());
        payload.put("transferType", "in");
        payload.put("transferAmount", amount);
        payload.put("content", content);
        payload.put("accountNumber", "SIMULATED");
        payload.put("gateway", "SIMULATED");
        TransactionIngestService.Outcome outcome = ingestService.ingest(payload);
        return amount + " VND, \"" + content + "\" -> " + outcome;
    }
}
