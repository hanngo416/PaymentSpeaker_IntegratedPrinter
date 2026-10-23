package controller.order;

import com.google.gson.JsonObject;
import controller.auth.LoginServlet;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import filter.RolePermissions;
import model.Attribution;
import model.enums.Permission;
import model.User;
import service.AffiliateService;

@WebServlet(name = "AffiliatePreviewServlet", urlPatterns = {"/orders/affiliate"})
public class AffiliatePreviewServlet extends HttpServlet {

    private final AffiliateService affiliateService = new AffiliateService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = (User) request.getSession().getAttribute(LoginServlet.SESSION_USER);
        JsonObject out = new JsonObject();
        try {
            long amount = parse(request.getParameter("amount"));
            Attribution at = affiliateService.attribute(request.getParameter("code"), user, amount);
            if (at == null) {
                out.addProperty("ok", false);
                out.addProperty("message", "Enter a referral code.");
            } else {
                out.addProperty("ok", true);
                boolean canSee = RolePermissions.has(user.getRole(), Permission.SALES_MANAGE);
                out.addProperty("message", at.getAffiliate().getName() + " - campaign "
                        + at.getCampaign().getName() + (canSee
                        ? ": " + at.getRateText() + ", estimated commission " + String.format("%,d", at.getCommission()) + " VND on "
                        + String.format("%,d", at.getEligibleAmount()) + " VND. The final amount is confirmed when the order is saved."
                        : ". The code is valid and will be credited when the order is saved."));
            }
        } catch (IllegalArgumentException e) {
            out.addProperty("ok", false);
            out.addProperty("message", e.getMessage());
        } catch (SQLException e) {
            throw new ServletException("Cannot check the affiliate code", e);
        }
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(out.toString());
    }

    private static long parse(String raw) {
        if (raw == null || !raw.matches("\\d{1,12}")) {
            return 0;
        }
        return Long.parseLong(raw);
    }
}
