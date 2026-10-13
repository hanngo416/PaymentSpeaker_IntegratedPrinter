package controller.speaker;

import dao.DeviceDAO;
import dao.TransactionDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.view.SpeakItem;

/**
 * Speaker screen: /speaker (page), /speaker/poll (JSON of amounts to read out), /speaker/ack (speaker confirms it read one).
 */
@WebServlet(name = "SpeakerServlet", urlPatterns = {"/speaker", "/speaker/poll", "/speaker/ack"})
public class SpeakerServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/speaker/speaker.jsp";
    private static final int BATCH_SIZE = 5;
    private static final int MAX_AGE_MINUTES = 5;

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if ("/speaker/poll".equals(request.getServletPath())) {
            writePollJson(response);
            return;
        }
        request.getRequestDispatcher(VIEW).forward(request, response);
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
        try {
            transactionDAO.markAnnounced(id);
        } catch (SQLException e) {
            throw new ServletException("Cannot mark the transaction as announced", e);
        }
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
    }

    private void writePollJson(HttpServletResponse response) throws ServletException, IOException {
        List<SpeakItem> items;
        try {
            deviceDAO.pingSpeaker();
            items = transactionDAO.findToAnnounce(BATCH_SIZE, MAX_AGE_MINUTES);
        } catch (SQLException e) {
            throw new ServletException("Cannot read the transactions waiting for the speaker", e);
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"id\":").append(items.get(i).getTransId())
                    .append(",\"amount\":").append(items.get(i).getAmount()).append('}');
        }
        json.append(']');
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(json.toString());
    }
}
