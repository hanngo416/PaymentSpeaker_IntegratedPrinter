import com.google.gson.JsonObject;
import dao.SettingDAO;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import model.User;
import model.enums.Role;
import model.view.DayProfit;
import service.AffiliateService;
import service.AgentTools;
import service.Features;
import service.ProfitService;
import util.DBUtil;

public class FeatureTest {

    private static int failures = 0;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static void setAll(SettingDAO settings, boolean tax, boolean online, boolean affiliate, boolean ai) throws Exception {
        settings.set(Features.settingKey(Features.TAX), tax ? "1" : "0");
        settings.set(Features.settingKey(Features.ONLINE), online ? "1" : "0");
        settings.set(Features.settingKey(Features.AFFILIATE), affiliate ? "1" : "0");
        settings.set(Features.settingKey(Features.AI), ai ? "1" : "0");
        Features.invalidateCache();
    }

    private static boolean hasTool(AgentTools tools, Role role, String name) {
        for (AgentTools.Tool t : tools.availableFor(role)) {
            if (t.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static void deleteSetting(String key) throws Exception {
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM Settings WHERE setting_key = ?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        }
    }

    public static void main(String[] args) throws Exception {
        SettingDAO settings = new SettingDAO();
        Map<String, String> saved = new HashMap<>();
        for (String module : Features.MODULES) {
            saved.put(Features.settingKey(module), settings.get(Features.settingKey(module), ""));
        }
        saved.put(Features.SETUP_DONE, settings.get(Features.SETUP_DONE, ""));
        AgentTools tools = new AgentTools();
        User admin = new User();
        admin.setRole(Role.ADMIN);
        admin.setUserId(1);
        try {
            setAll(settings, true, true, true, true);
            for (String module : Features.MODULES) {
                check("default on: " + module, Features.isOn(module), true);
            }
            check("nothing disabled: tax page", Features.disabledModule("/tax/profile"), null);
            check("nothing disabled: sales", Features.disabledModule("/sales/import"), null);
            check("prompt note is empty when all modules are on", Features.promptNote(), "");

            setAll(settings, false, true, true, true);
            check("tax off: /tax", Features.disabledModule("/tax"), "tax");
            check("tax off: /tax/calculator", Features.disabledModule("/tax/calculator"), "tax");
            check("tax off: /admin/taxes", Features.disabledModule("/admin/taxes"), "tax");
            check("tax off: e-invoice list", Features.disabledModule("/invoices/manage"), "tax");
            check("tax off: shift statistics stay", Features.disabledModule("/invoices"), null);
            check("tax off: POS stays", Features.disabledModule("/orders/new"), null);
            check("tax off: guide stays", Features.disabledModule("/guide/article"), null);
            check("tax off: bot feed is handled by its servlet", Features.disabledModule("/bot/feed"), null);
            check("tax off: tax_estimate hidden", hasTool(tools, Role.ADMIN, "tax_estimate"), false);
            check("tax off: tax_rules hidden", hasTool(tools, Role.ADMIN, "tax_rules"), false);
            check("tax off: search_tax_docs hidden", hasTool(tools, Role.ADMIN, "search_tax_docs"), false);
            check("tax off: revenue_summary stays", hasTool(tools, Role.ADMIN, "revenue_summary"), true);
            JsonObject refused = tools.run(admin, "tax_estimate", new JsonObject());
            check("tax off: tax_estimate refused at run time", refused.has("error"), true);
            check("tax off: prompt note names the module", Features.promptNote().contains("tax"), true);
            Timestamp from = Timestamp.valueOf("2026-01-01 00:00:00");
            Timestamp to = Timestamp.valueOf("2026-12-31 23:59:59");
            BigDecimal reserve = BigDecimal.ZERO;
            for (DayProfit d : new ProfitService().byDay(from, to, null)) {
                reserve = reserve.add(d.getTaxReserve());
            }
            check("tax off: profit has no tax reserve", reserve.signum(), 0);

            setAll(settings, true, false, true, true);
            check("online off: /sales", Features.disabledModule("/sales"), "online");
            check("online off: /sales/import", Features.disabledModule("/sales/import"), "online");
            check("online off: /analytics", Features.disabledModule("/analytics"), "online");
            check("online off: channel payouts", Features.disabledModule("/admin/channel-payouts"), "online");
            check("online off: channel reports", Features.disabledModule("/admin/channel-reports"), "online");
            check("online off: partners page stays while affiliate is on", Features.disabledModule("/admin/affiliates"), null);
            check("online off: tax page stays", Features.disabledModule("/tax"), null);

            setAll(settings, true, true, false, true);
            check("affiliate off: campaigns", Features.disabledModule("/admin/campaigns"), "affiliate");
            check("affiliate off: portal", Features.disabledModule("/affiliate/portal"), "affiliate");
            check("affiliate off: POS referral lookup", Features.disabledModule("/orders/affiliate"), "affiliate");
            check("affiliate off: commissions", Features.disabledModule("/admin/commissions"), "affiliate");
            check("affiliate off: channel commissions are another module", Features.disabledModule("/admin/channel-commissions"), null);
            check("affiliate off: partners page stays while online is on", Features.disabledModule("/admin/affiliates"), null);
            User cashier = new User();
            cashier.setRole(Role.CASHIER);
            cashier.setUserId(1);
            check("affiliate off: a referral code is ignored", new AffiliateService().attribute("ABC123", cashier, 100000L), null);

            setAll(settings, true, false, false, true);
            check("online and affiliate off: partners page blocked", Features.disabledModule("/admin/affiliates"), "affiliate");

            setAll(settings, true, true, true, false);
            check("ai off: /bot/chat", Features.disabledModule("/bot/chat"), "ai");
            check("ai off: bot feed stays", Features.disabledModule("/bot/feed"), null);
            check("ai off: tax tools stay", hasTool(tools, Role.ADMIN, "tax_estimate"), true);

            settings.set(Features.SETUP_DONE, "1");
            Features.invalidateCache();
            check("setup done", Features.setupPending(), false);
            deleteSetting(Features.SETUP_DONE);
            Features.invalidateCache();
            check("setup pending without the key", Features.setupPending(), true);
        } finally {
            for (Map.Entry<String, String> e : saved.entrySet()) {
                if (e.getValue().isEmpty()) {
                    deleteSetting(e.getKey());
                } else {
                    settings.set(e.getKey(), e.getValue());
                }
            }
            Features.invalidateCache();
        }
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
