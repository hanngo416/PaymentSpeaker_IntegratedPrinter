package controller.guide;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import controller.auth.LoginServlet;
import dao.GuideDAO;
import filter.AccessPolicy;
import filter.RolePermissions;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.GuideArticle;
import model.GuideCategory;
import model.User;
import model.enums.Permission;
import service.BusinessDocService;

@WebServlet(name = "KnowledgeServlet", urlPatterns = {"/guide", "/guide/article", "/guide/search", "/guide/progress"})
public class KnowledgeServlet extends HttpServlet {

    private static final String OVERVIEW = "/WEB-INF/views/guide/overview.jsp";
    private static final String ARTICLE = "/WEB-INF/views/guide/article.jsp";
    private static final List<String> MODELS = Arrays.asList("OFFLINE_STORE", "SHOPEE_SELLER", "TIKTOK_SELLER", "OMNICHANNEL");
    private static final int SEARCH_LIMIT = 20;

    private final BusinessDocService docs = new BusinessDocService();
    private final GuideDAO guideDAO = new GuideDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String path = request.getServletPath();
        try {
            if ("/guide/article".equals(path)) {
                showArticle(request, response, user);
            } else if ("/guide/progress".equals(path)) {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            } else if ("/guide/search".equals(path) && "json".equals(request.getParameter("format"))) {
                searchJson(request, response);
            } else {
                showOverview(request, response, user);
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load the guide progress", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/guide/progress".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        User user = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        JsonObject out = new JsonObject();
        response.setContentType("application/json;charset=UTF-8");
        String articleId = param(request, "article");
        String kind = param(request, "kind");
        String state = param(request, "state");
        GuideArticle article = docs.getArticleById(articleId);
        String storedId = articleId;
        String problem = null;
        if (article == null) {
            problem = "Unknown article";
        } else if (GuideDAO.BOOKMARK.equals(kind)) {
            if (!"SAVED".equals(state) && !"UNCHECKED".equals(state)) {
                problem = "Invalid state";
            }
        } else if (GuideDAO.READ.equals(kind)) {
            if (!"CHECKED".equals(state) && !"UNCHECKED".equals(state)) {
                problem = "Invalid state";
            }
        } else if (GuideDAO.CHECKLIST.equals(kind)) {
            String item = param(request, "item");
            if (!"CHECKED".equals(state) && !"UNCHECKED".equals(state)) {
                problem = "Invalid state";
            } else if (!item.matches("\\d{1,3}") || Integer.parseInt(item) >= article.getChecklist().size()) {
                problem = "Unknown checklist item";
            } else {
                storedId = checklistKey(articleId, Integer.parseInt(item));
            }
        } else {
            problem = "Invalid kind";
        }
        if (problem != null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.addProperty("ok", false);
            out.addProperty("error", problem);
            response.getWriter().write(out.toString());
            return;
        }
        try {
            guideDAO.save(user.getUserId(), storedId, kind, state);
            out.addProperty("ok", true);
            if (GuideDAO.CHECKLIST.equals(kind)) {
                Map<String, String> states = guideDAO.load(user.getUserId());
                out.addProperty("checked", countChecked(article, states));
                out.addProperty("total", article.getChecklist().size());
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot save the guide progress", e);
        }
        response.getWriter().write(out.toString());
    }

    public static String checklistKey(String articleId, int index) {
        return articleId + "#" + index;
    }

    private static int countChecked(GuideArticle article, Map<String, String> states) {
        int n = 0;
        for (int i = 0; i < article.getChecklist().size(); i++) {
            if ("CHECKED".equals(states.get(GuideDAO.key(GuideDAO.CHECKLIST, checklistKey(article.getId(), i))))) {
                n++;
            }
        }
        return n;
    }

    private void showOverview(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        Map<String, String> states = guideDAO.load(user.getUserId());
        String q = param(request, "q");
        if (q.length() > BusinessDocService.MAX_QUERY) {
            q = q.substring(0, BusinessDocService.MAX_QUERY);
        }
        String model = param(request, "model");
        if (!MODELS.contains(model)) {
            model = "";
        }
        String categoryId = param(request, "category");
        if (docs.getCategory(categoryId) == null) {
            categoryId = "";
        }
        boolean savedOnly = "1".equals(request.getParameter("saved"));

        List<GuideArticle> base = q.isEmpty() ? docs.getArticles() : docs.search(q, SEARCH_LIMIT);
        List<GuideArticle> shown = new ArrayList<>();
        for (GuideArticle a : base) {
            if (!model.isEmpty() && !a.getTargetModels().isEmpty() && !a.getTargetModels().contains(model)) {
                continue;
            }
            if (!categoryId.isEmpty() && !a.getCategoryId().equals(categoryId)) {
                continue;
            }
            if (savedOnly && !"SAVED".equals(states.get(GuideDAO.key(GuideDAO.BOOKMARK, a.getId())))) {
                continue;
            }
            shown.add(a);
        }

        Map<String, Integer> catTotal = new HashMap<>();
        Map<String, Integer> catRead = new HashMap<>();
        Map<String, Boolean> read = new HashMap<>();
        Map<String, Boolean> saved = new HashMap<>();
        GuideArticle next = null;
        int readCount = 0;
        for (GuideCategory c : docs.getCategories()) {
            for (GuideArticle a : docs.getArticlesByCategory(c.getId())) {
                boolean isRead = "CHECKED".equals(states.get(GuideDAO.key(GuideDAO.READ, a.getId())));
                read.put(a.getId(), isRead);
                saved.put(a.getId(), "SAVED".equals(states.get(GuideDAO.key(GuideDAO.BOOKMARK, a.getId()))));
                catTotal.merge(c.getId(), 1, Integer::sum);
                if (isRead) {
                    catRead.merge(c.getId(), 1, Integer::sum);
                    readCount++;
                } else if (next == null) {
                    next = a;
                }
            }
        }
        int total = docs.getArticles().size();
        request.setAttribute("available", docs.isAvailable());
        request.setAttribute("categories", docs.getCategories());
        request.setAttribute("articles", shown);
        request.setAttribute("catTotal", catTotal);
        request.setAttribute("catRead", catRead);
        request.setAttribute("read", read);
        request.setAttribute("saved", saved);
        request.setAttribute("nextArticle", next);
        request.setAttribute("readCount", readCount);
        request.setAttribute("totalCount", total);
        request.setAttribute("percent", total == 0 ? 0 : readCount * 100 / total);
        request.setAttribute("q", q);
        request.setAttribute("model", model);
        request.setAttribute("category", categoryId);
        request.setAttribute("savedOnly", savedOnly);
        request.setAttribute("filtered", !q.isEmpty() || !model.isEmpty() || !categoryId.isEmpty() || savedOnly);
        request.getRequestDispatcher(OVERVIEW).forward(request, response);
    }

    private void showArticle(HttpServletRequest request, HttpServletResponse response, User user)
            throws SQLException, ServletException, IOException {
        GuideArticle article = docs.getArticleById(request.getParameter("id"));
        if (article == null) {
            article = docs.getArticleBySlug(request.getParameter("slug"));
        }
        if (article == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Map<String, String> states = guideDAO.load(user.getUserId());
        List<Boolean> checked = new ArrayList<>();
        for (int i = 0; i < article.getChecklist().size(); i++) {
            checked.add("CHECKED".equals(states.get(GuideDAO.key(GuideDAO.CHECKLIST, checklistKey(article.getId(), i)))));
        }
        request.setAttribute("article", article);
        request.setAttribute("category", docs.getCategory(article.getCategoryId()));
        request.setAttribute("checked", checked);
        request.setAttribute("checkedCount", countChecked(article, states));
        request.setAttribute("isRead", "CHECKED".equals(states.get(GuideDAO.key(GuideDAO.READ, article.getId()))));
        request.setAttribute("isSaved", "SAVED".equals(states.get(GuideDAO.key(GuideDAO.BOOKMARK, article.getId()))));
        request.setAttribute("actionOk", actionAllowed(article.getActionUrl(), user));
        List<GuideArticle> related = new ArrayList<>();
        for (GuideArticle a : docs.getArticlesByCategory(article.getCategoryId())) {
            if (!a.getId().equals(article.getId()) && related.size() < 3) {
                related.add(a);
            }
        }
        request.setAttribute("related", related);
        request.getRequestDispatcher(ARTICLE).forward(request, response);
    }

    private void searchJson(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonArray rows = new JsonArray();
        for (GuideArticle a : docs.search(param(request, "q"), SEARCH_LIMIT)) {
            JsonObject o = new JsonObject();
            o.addProperty("id", a.getId());
            o.addProperty("title", a.getTitle());
            o.addProperty("summary", a.getSummary());
            rows.add(o);
        }
        JsonObject out = new JsonObject();
        out.add("rows", rows);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(out.toString());
    }

    static boolean actionAllowed(String url, User user) {
        if (url == null || !url.startsWith("/") || url.startsWith("//")) {
            return false;
        }
        int cut = url.indexOf('?');
        String path = cut < 0 ? url : url.substring(0, cut);
        Permission needed = AccessPolicy.requiredFor(path, "GET");
        return needed == null || RolePermissions.has(user.getRole(), needed);
    }

    private static String param(HttpServletRequest request, String name) {
        String v = request.getParameter(name);
        return v == null ? "" : v.trim();
    }
}
