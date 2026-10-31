import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import model.User;
import model.enums.Role;
import service.AgentTools;

public class AgentTest {

    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "OK   " : "FAIL ") + name);
        if (!ok) {
            failed++;
        }
    }

    static List<String> names(AgentTools tools, Role role) {
        List<String> list = new ArrayList<>();
        for (AgentTools.Tool t : tools.availableFor(role)) {
            list.add(t.getName());
        }
        return list;
    }

    static JsonObject queryOf(String q) {
        JsonObject o = new JsonObject();
        o.addProperty("query", q);
        return o;
    }

    static User user(Role role) {
        User u = new User();
        u.setRole(role);
        return u;
    }

    public static void main(String[] args) {
        AgentTools tools = new AgentTools();
        check("cashier has search_orders", names(tools, Role.CASHIER).contains("search_orders"));
        check("cashier has no list_alerts", !names(tools, Role.CASHIER).contains("list_alerts"));
        check("cashier has no tax_estimate", !names(tools, Role.CASHIER).contains("tax_estimate"));
        check("cashier has no revenue_summary", !names(tools, Role.CASHIER).contains("revenue_summary"));
        check("auditor has list_alerts", names(tools, Role.AUDITOR).contains("list_alerts"));
        check("auditor has no search_orders", !names(tools, Role.AUDITOR).contains("search_orders"));
        check("manager has tax_estimate", names(tools, Role.STORE_MANAGER).contains("tax_estimate"));
        check("technician list_devices and docs", names(tools, Role.TECHNICIAN).size() == 3 && names(tools, Role.TECHNICIAN).contains("list_devices") && names(tools, Role.TECHNICIAN).contains("search_starter_guide"));
        check("cashier has no list_users", !names(tools, Role.CASHIER).contains("list_users"));
        check("manager has no list_devices extra check", names(tools, Role.STORE_MANAGER).contains("list_users"));
        check("admin has all 14", names(tools, Role.ADMIN).size() == 14);

        JsonObject r = tools.run(user(Role.CASHIER), "list_alerts", new JsonObject());
        check("cashier list_alerts refused at run time", r.has("error"));
        r = tools.run(user(Role.ADMIN), "drop_table", new JsonObject());
        check("unknown function refused", r.has("error"));
        JsonObject bad = new JsonObject();
        bad.addProperty("period", "forever");
        r = tools.run(user(Role.ADMIN), "revenue_summary", bad);
        check("bad period refused", r.has("error"));
        bad = new JsonObject();
        bad.addProperty("status", "x'; DROP TABLE Orders;--");
        r = tools.run(user(Role.ADMIN), "search_orders", bad);
        check("bad status refused", r.has("error"));

        r = tools.run(user(Role.CASHIER), "search_tax_docs", queryOf("mo quan ca phe can giay phep gi"));
        check("docs: cafe query finds permits", r.has("rows") && r.getAsJsonArray("rows").toString().contains("an toàn thực phẩm"));
        r = tools.run(user(Role.CASHIER), "search_tax_docs", queryOf("nguong 1 ty"));
        check("docs: result carries official links", r.has("rows") && r.getAsJsonArray("rows").toString().contains("https://") && !r.toString().contains("http://"));
        r = tools.run(user(Role.CASHIER), "search_tax_docs", queryOf("thue khoan 2026"));
        check("docs: accent-free query matches", r.has("rows"));
        r = tools.run(user(Role.CASHIER), "search_tax_docs", queryOf("zzzqqq"));
        check("docs: unknown query lists topics", r.has("topics") && !r.has("rows"));

        r = tools.run(user(Role.CASHIER), "search_starter_guide", queryOf("shopee"));
        check("guide: shopee query finds seller articles", r.has("rows") && r.getAsJsonArray("rows").size() > 0);
        r = tools.run(user(Role.CASHIER), "search_starter_guide", queryOf("dang ky kinh doanh"));
        check("guide: registration finds legal guide", r.has("rows") && r.getAsJsonArray("rows").toString().contains("legal-"));

        System.out.println(failed == 0 ? "ALL OK" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
