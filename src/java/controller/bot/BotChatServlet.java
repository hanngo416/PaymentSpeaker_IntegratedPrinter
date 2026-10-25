package controller.bot;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import controller.auth.LoginServlet;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.User;
import service.AgentService;

@WebServlet(name = "BotChatServlet", urlPatterns = {"/bot/chat"})
public class BotChatServlet extends HttpServlet {

    private final AgentService agent = new AgentService();

    private static JsonArray parseHistory(String raw) {
        if (raw == null || raw.length() > 20000) {
            return null;
        }
        try {
            return new com.google.gson.JsonParser().parse(raw).getAsJsonArray();
        } catch (RuntimeException e) {
            return null;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String message = request.getParameter("message");
        JsonObject out = new JsonObject();
        if (message == null || message.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.addProperty("answer", "Type a question first");
        } else if (message.length() > AgentService.MAX_MESSAGE) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.addProperty("answer", "Question is too long (max " + AgentService.MAX_MESSAGE + " characters)");
        } else {
            String page = request.getParameter("page");
            String pageTitle = request.getParameter("pageTitle");
            AgentService.Reply reply = agent.ask(user, message.trim(), parseHistory(request.getParameter("history")), page, pageTitle);
            out.addProperty("answer", reply.getAnswer());
            out.addProperty("fallback", reply.isFallback());
            JsonArray steps = new JsonArray();
            for (String s : reply.getSteps()) {
                steps.add(new com.google.gson.JsonPrimitive(s));
            }
            out.add("steps", steps);
            JsonArray links = new JsonArray();
            for (String[] l : reply.getLinks()) {
                JsonObject o = new JsonObject();
                o.addProperty("title", l[0]);
                o.addProperty("url", l[1]);
                links.add(o);
            }
            out.add("links", links);
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(out.toString());
    }
}
