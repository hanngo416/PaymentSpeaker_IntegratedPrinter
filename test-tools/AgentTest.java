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
        check("technician only list_devices", names(tools, Role.TECHNICIAN).size() == 1 && names(tools, Role.TECHNICIAN).contains("list_devices"));
        check("cashier has no list_users", !names(tools, Role.CASHIER).contains("list_users"));
        check("manager has no list_devices extra check", names(tools, Role.STORE_MANAGER).contains("list_users"));
        check("admin has all 12", names(tools, Role.ADMIN).size() == 12);

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

        System.out.println(failed == 0 ? "ALL OK" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
