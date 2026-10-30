package service;

import dao.SettingDAO;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Features {

    public static final String TAX = "tax";
    public static final String ONLINE = "online";
    public static final String AFFILIATE = "affiliate";
    public static final String AI = "ai";
    public static final String[] MODULES = {TAX, ONLINE, AFFILIATE, AI};
    public static final String SETUP_DONE = "setup_done";

    private static final Logger LOG = Logger.getLogger(Features.class.getName());
    private static final long TTL_MS = 10000L;

    private static final String[] TAX_PATHS = {"/tax", "/admin/taxes", "/invoices/manage", "/invoices/new", "/invoices/export"};
    private static final String[] ONLINE_PATHS = {"/sales", "/analytics", "/admin/channel-commissions", "/admin/channel-payouts", "/admin/channel-reports"};
    private static final String[] AFFILIATE_PATHS = {"/admin/affiliate-overview", "/admin/campaigns", "/admin/referrals",
        "/admin/commissions", "/admin/payouts", "/affiliate/portal", "/orders/affiliate"};
    private static final String[] AI_PATHS = {"/bot/chat"};
    private static final String SHARED_PATH = "/admin/affiliates";

    private static volatile Map<String, Boolean> cache;
    private static volatile boolean setupPending;
    private static volatile long loadedAt;

    private Features() {
    }

    public static String settingKey(String module) {
        return "feature_" + module;
    }

    public static void invalidateCache() {
        loadedAt = 0L;
    }

    public static Map<String, Boolean> snapshot() {
        long now = System.currentTimeMillis();
        Map<String, Boolean> current = cache;
        if (current == null || now - loadedAt > TTL_MS) {
            current = load();
            cache = current;
            loadedAt = now;
        }
        return current;
    }

    public static boolean isOn(String module) {
        Boolean on = snapshot().get(module);
        return on == null || on;
    }

    public static boolean setupPending() {
        snapshot();
        return setupPending;
    }

    public static String moduleOf(String path) {
        if (path == null) {
            return null;
        }
        if (matches(path, TAX_PATHS)) {
            return TAX;
        }
        if (matches(path, ONLINE_PATHS)) {
            return ONLINE;
        }
        if (matches(path, AFFILIATE_PATHS)) {
            return AFFILIATE;
        }
        if (matches(path, AI_PATHS)) {
            return AI;
        }
        return null;
    }

    public static String disabledModule(String path) {
        if (path != null && (path.equals(SHARED_PATH) || path.startsWith(SHARED_PATH + "/"))) {
            return isOn(AFFILIATE) || isOn(ONLINE) ? null : AFFILIATE;
        }
        String module = moduleOf(path);
        return module != null && !isOn(module) ? module : null;
    }

    public static String promptNote() {
        StringBuilder off = new StringBuilder();
        for (String module : MODULES) {
            if (!isOn(module)) {
                off.append(off.length() == 0 ? "" : ", ").append(module);
            }
        }
        if (off.length() == 0) {
            return "";
        }
        return " These modules are turned off in this store: " + off + ". Do not suggest, describe or open their pages and do not answer"
                + " questions about them; say that the module is off and that an administrator can turn it on in /settings.";
    }

    private static boolean matches(String path, String[] prefixes) {
        for (String prefix : prefixes) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Boolean> load() {
        Map<String, Boolean> map = new LinkedHashMap<>();
        SettingDAO settings = new SettingDAO();
        try {
            for (String module : MODULES) {
                map.put(module, !"0".equals(settings.get(settingKey(module), "1").trim()));
            }
            setupPending = settings.get(SETUP_DONE, "").trim().isEmpty();
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "FEATURES_LOAD_FAILED {0}", e.getMessage());
            for (String module : MODULES) {
                map.put(module, Boolean.TRUE);
            }
            setupPending = false;
        }
        return Collections.unmodifiableMap(map);
    }
}
