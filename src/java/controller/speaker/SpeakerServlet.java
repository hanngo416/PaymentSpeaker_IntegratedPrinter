package controller.speaker;

import dao.TransactionDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.view.SpeakItem;

@WebServlet(name = "SpeakerServlet", urlPatterns = {"/speaker/poll", "/speaker/ack"})
public class SpeakerServlet extends HttpServlet {

    private static final int BATCH_SIZE = 5;
    private static final int MAX_AGE_MINUTES = 5;

    private static final int MAX_BANK_AGE_MINUTES = 10;

    private final TransactionDAO transactionDAO = new TransactionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        writePollJson(response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/speaker/ack".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        int id;
        try {
            id = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean first;
        try {
            first = transactionDAO.markAnnounced(id);
        } catch (SQLException e) {
            throw new ServletException("Cannot mark the transaction as announced", e);
        }
        response.setStatus(first ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_CONFLICT);
    }

    private void writePollJson(HttpServletResponse response) throws ServletException, IOException {
        List<SpeakItem> items;
        try {
            items = transactionDAO.findToAnnounce(BATCH_SIZE, MAX_AGE_MINUTES, MAX_BANK_AGE_MINUTES);
        } catch (SQLException e) {
            throw new ServletException("Cannot read the transactions waiting for the popup", e);
        }
        SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            SpeakItem item = items.get(i);
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(item.getTransId())
                    .append(",\"amount\":").append(item.getAmount())
                    .append(",\"order\":\"").append(escape(item.getOrderCode()))
                    .append("\",\"time\":\"").append(time.format(item.getTransTime())).append("\"}");
        }
        json.append(']');
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json.toString());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
