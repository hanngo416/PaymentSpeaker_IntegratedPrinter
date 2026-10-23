package controller.admin;

import controller.auth.LoginServlet;
import dao.CatalogDAO;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Category;
import model.Group;
import model.Page;
import model.User;
import service.AuditService;
import util.SqlErrors;

@WebServlet(name = "CategoryServlet", urlPatterns = {"/admin/categories", "/admin/groups"})
public class CategoryServlet extends HttpServlet {

    private static final String VIEW_CATEGORIES = "/WEB-INF/views/admin/categories.jsp";
    private static final String VIEW_GROUPS = "/WEB-INF/views/admin/groups.jsp";
    private static final int PAGE_SIZE = 10;
    private static final Map<String, String> MESSAGES = new HashMap<>();

    static {
        MESSAGES.put("catAdded", "Category added.");
        MESSAGES.put("catSaved", "Category saved.");
        MESSAGES.put("catDeleted", "Category deleted.");
        MESSAGES.put("catForced", "Category deleted together with its groups and products.");
        MESSAGES.put("catNotEmpty", "This category still has groups. Delete them first, or use Force delete.");
        MESSAGES.put("groupAdded", "Group added.");
        MESSAGES.put("groupSaved", "Group saved.");
        MESSAGES.put("groupDeleted", "Group deleted.");
        MESSAGES.put("groupNotEmpty", "This group still has products. Delete or move them first.");
        MESSAGES.put("badName", "The name must be 2-50 characters and the order a number from 0 to 9999.");
        MESSAGES.put("dup", "This name already exists.");
        MESSAGES.put("groupForced", "Group deleted together with its products.");
        MESSAGES.put("confirm", "The name you typed does not match. Nothing was deleted.");
        MESSAGES.put("bad", "Invalid request.");
    }

    private final CatalogDAO catalogDAO = new CatalogDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        boolean groupsTab = request.getServletPath().equals("/admin/groups");
        int page = 1;
        try {
            page = Math.max(1, Integer.parseInt(request.getParameter("page")));
        } catch (NumberFormatException e) {
        }
        String q = trim(request.getParameter("q"));
        String rawCategory = trim(request.getParameter("category"));
        Integer categoryFilter = rawCategory.matches("\\d{1,9}") ? Integer.valueOf(rawCategory) : null;
        try {
            List<Category> categories = catalogDAO.findAllCategories();
            request.setAttribute("categories", categories);
            request.setAttribute("groupCounts", catalogDAO.countGroupsByCategory());
            request.setAttribute("categoryProductCounts", catalogDAO.countProductsByCategory());
            request.setAttribute("productCounts", catalogDAO.countProductsByGroup());
            String needle = q.toLowerCase();
            if (groupsTab) {
                List<Group> found = new ArrayList<>();
                for (Group g : catalogDAO.findAllGroups()) {
                    if ((categoryFilter == null || g.getCategoryId() == categoryFilter)
                            && (needle.isEmpty() || g.getGroupName().toLowerCase().contains(needle))) {
                        found.add(g);
                    }
                }
                request.setAttribute("result", pageOf(found, page));
            } else {
                List<Category> found = new ArrayList<>();
                for (Category c : categories) {
                    if (needle.isEmpty() || c.getCategoryName().toLowerCase().contains(needle)) {
                        found.add(c);
                    }
                }
                request.setAttribute("result", pageOf(found, page));
            }
        } catch (SQLException e) {
            throw new ServletException("Cannot load categories", e);
        }
        request.setAttribute("q", q);
        request.setAttribute("categoryFilter", categoryFilter);
        request.setAttribute("message", MESSAGES.get(request.getParameter("msg")));
        request.getRequestDispatcher(groupsTab ? VIEW_GROUPS : VIEW_CATEGORIES).forward(request, response);
    }

    private static <T> Page<T> pageOf(List<T> all, int requested) {
        int pages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(requested, pages);
        int from = (page - 1) * PAGE_SIZE;
        return new Page<>(all.subList(from, Math.min(all.size(), from + PAGE_SIZE)), all.size(), page, PAGE_SIZE);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User actor = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        String msg = "bad";
        try {
            msg = handle(request, actor);
        } catch (NumberFormatException e) {
            msg = "bad";
        } catch (SQLException e) {
            if (!SqlErrors.isDuplicateKey(e)) {
                throw new ServletException("Cannot update the categories", e);
            }
            msg = "dup";
        }
        boolean groupAction = String.valueOf(request.getParameter("action")).toLowerCase().contains("group");
        response.sendRedirect(request.getContextPath() + (groupAction ? "/admin/groups" : "/admin/categories") + "?msg=" + msg);
    }

    private String handle(HttpServletRequest request, User actor) throws SQLException {
        String action = String.valueOf(request.getParameter("action"));
        String name = trim(request.getParameter("name"));
        String rawOrder = trim(request.getParameter("displayOrder"));
        boolean nameOk = name.length() >= 2 && name.length() <= 50 && (rawOrder.isEmpty() || rawOrder.matches("\\d{1,4}"));
        int order = rawOrder.isEmpty() ? 0 : Integer.parseInt(rawOrder);

        switch (action) {
            case "addCategory":
                if (!nameOk) {
                    return "badName";
                }
                catalogDAO.insertCategory(name, order);
                AuditService.log(actor, "CATEGORY_CREATE", "CATEGORY", name);
                return "catAdded";
            case "saveCategory":
                if (!nameOk) {
                    return "badName";
                }
                catalogDAO.updateCategory(Integer.parseInt(request.getParameter("id")), name, order);
                AuditService.log(actor, "CATEGORY_UPDATE", "CATEGORY", name);
                return "catSaved";
            case "deleteCategory":
                if (catalogDAO.deleteEmptyCategory(Integer.parseInt(request.getParameter("id")))) {
                    AuditService.log(actor, "CATEGORY_DELETE", "CATEGORY", request.getParameter("id"));
                    return "catDeleted";
                }
                return "catNotEmpty";
            case "forceDeleteCategory":
                return forceDelete(request, actor);
            case "forceDeleteGroup":
                return forceDeleteGroup(request, actor);
            case "addGroup":
                if (!nameOk) {
                    return "badName";
                }
                catalogDAO.insertGroup(Integer.parseInt(request.getParameter("categoryId")), name, order);
                AuditService.log(actor, "GROUP_CREATE", "GROUP", name);
                return "groupAdded";
            case "saveGroup":
                if (!nameOk) {
                    return "badName";
                }
                catalogDAO.updateGroup(Integer.parseInt(request.getParameter("id")), Integer.parseInt(request.getParameter("categoryId")), name, order);
                AuditService.log(actor, "GROUP_UPDATE", "GROUP", name);
                return "groupSaved";
            case "deleteGroup":
                if (catalogDAO.deleteEmptyGroup(Integer.parseInt(request.getParameter("id")))) {
                    AuditService.log(actor, "GROUP_DELETE", "GROUP", request.getParameter("id"));
                    return "groupDeleted";
                }
                return "groupNotEmpty";
            default:
                return "bad";
        }
    }

    private String forceDelete(HttpServletRequest request, User actor) throws SQLException {
        int id = Integer.parseInt(request.getParameter("id"));
        Category category = null;
        for (Category c : catalogDAO.findAllCategories()) {
            if (c.getCategoryId() == id) {
                category = c;
            }
        }
        if (category == null) {
            return "bad";
        }
        if (!category.getCategoryName().equals(trim(request.getParameter("confirmName")))) {
            return "confirm";
        }
        int[] removed = catalogDAO.forceDeleteCategory(id);
        AuditService.log(actor, "CATEGORY_FORCE_DELETE", "CATEGORY", category.getCategoryName(),
                "groups", String.valueOf(removed[0]), "products", String.valueOf(removed[1]));
        return "catForced";
    }

    private String forceDeleteGroup(HttpServletRequest request, User actor) throws SQLException {
        int id = Integer.parseInt(request.getParameter("id"));
        Group group = null;
        for (Group g : catalogDAO.findAllGroups()) {
            if (g.getGroupId() == id) {
                group = g;
            }
        }
        if (group == null) {
            return "bad";
        }
        if (!group.getGroupName().equals(trim(request.getParameter("confirmName")))) {
            return "confirm";
        }
        int removed = catalogDAO.forceDeleteGroup(id);
        AuditService.log(actor, "GROUP_FORCE_DELETE", "GROUP", group.getGroupName(), "products", String.valueOf(removed));
        return "groupForced";
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
