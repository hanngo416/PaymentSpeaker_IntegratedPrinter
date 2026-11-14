import filter.AccessPolicy;
import filter.CsrfToken;
import filter.RolePermissions;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import model.enums.Permission;
import model.enums.Role;

public class AccessTest {

    private static int failures = 0;

    private static void check(String name, Object got, Object expected) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(got));
        if (!ok) {
            failures++;
        }
        System.out.println((ok ? "OK   " : "FAIL ") + name + " -> " + got + (ok ? "" : " (expected " + expected + ")"));
    }

    private static boolean allowed(Role role, String method, String path) {
        Permission need = AccessPolicy.requiredFor(path, method);
        return need == null || RolePermissions.has(role, need);
    }

    private static void access(Role role, String method, String path, boolean expected) {
        check(role + " " + method + " " + path, allowed(role, method, path), expected);
    }

    private static HttpSession session() {
        final Map<String, Object> attrs = new HashMap<>();
        return (HttpSession) Proxy.newProxyInstance(AccessTest.class.getClassLoader(), new Class<?>[]{HttpSession.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] a) {
                if (m.getName().equals("getAttribute")) {
                    return attrs.get((String) a[0]);
                }
                if (m.getName().equals("setAttribute")) {
                    attrs.put((String) a[0], a[1]);
                }
                return null;
            }
        });
    }

    private static HttpServletRequest request(final String header, final String param) {
        return (HttpServletRequest) Proxy.newProxyInstance(AccessTest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method m, Object[] a) {
                if (m.getName().equals("getHeader") && CsrfToken.HEADER.equals(a[0])) {
                    return header;
                }
                if (m.getName().equals("getParameter") && CsrfToken.PARAM.equals(a[0])) {
                    return param;
                }
                return null;
            }
        });
    }

    public static void main(String[] args) {
        check("public: /login", AccessPolicy.isPublic("/login"), true);
        check("public: /login/verify", AccessPolicy.isPublic("/login/verify"), true);
        check("public: /health", AccessPolicy.isPublic("/health"), true);
        check("public: /assets/css/base.css", AccessPolicy.isPublic("/assets/css/base.css"), true);
        check("not public: /loginx", AccessPolicy.isPublic("/loginx"), false);
        check("not public: /home", AccessPolicy.isPublic("/home"), false);
        check("not public: /admin/users", AccessPolicy.isPublic("/admin/users"), false);

        String[] anyPaths = {"/home", "/orders/new", "/admin/users", "/admin/audit", "/tax", "/sales", "/unknown/page"};
        for (String p : anyPaths) {
            access(Role.ADMIN, "GET", p, true);
            access(Role.ADMIN, "POST", p, true);
        }

        access(Role.CASHIER, "GET", "/orders/new", true);
        access(Role.CASHIER, "POST", "/orders/new", true);
        access(Role.CASHIER, "GET", "/shift", true);
        access(Role.CASHIER, "GET", "/transactions", true);
        access(Role.CASHIER, "GET", "/speaker/poll", true);
        access(Role.CASHIER, "GET", "/home", true);
        access(Role.CASHIER, "GET", "/settings", true);
        access(Role.CASHIER, "GET", "/admin/users", false);
        access(Role.CASHIER, "POST", "/admin/users", false);
        access(Role.CASHIER, "GET", "/admin/products", false);
        access(Role.CASHIER, "GET", "/admin/audit", false);
        access(Role.CASHIER, "GET", "/admin/devices", false);
        access(Role.CASHIER, "GET", "/alerts", false);
        access(Role.CASHIER, "GET", "/transactions/review", false);
        access(Role.CASHIER, "POST", "/transactions/review", false);
        access(Role.CASHIER, "GET", "/tax", false);
        access(Role.CASHIER, "GET", "/sales", false);
        access(Role.CASHIER, "GET", "/invoices", false);
        access(Role.CASHIER, "GET", "/reports/shifts", false);
        access(Role.CASHIER, "GET", "/admin", false);
        access(Role.CASHIER, "GET", "/unknown/page", false);
        access(Role.CASHIER, "GET", "/ordersx", false);

        access(Role.STORE_MANAGER, "GET", "/admin/users", true);
        access(Role.STORE_MANAGER, "POST", "/admin/users", true);
        access(Role.STORE_MANAGER, "GET", "/admin/users/edit", true);
        access(Role.STORE_MANAGER, "GET", "/admin/products", true);
        access(Role.STORE_MANAGER, "POST", "/admin/discounts", true);
        access(Role.STORE_MANAGER, "GET", "/transactions/review", true);
        access(Role.STORE_MANAGER, "POST", "/transactions/review", true);
        access(Role.STORE_MANAGER, "GET", "/tax", true);
        access(Role.STORE_MANAGER, "GET", "/admin/taxes", true);
        access(Role.STORE_MANAGER, "POST", "/admin/taxes", false);
        access(Role.STORE_MANAGER, "POST", "/tax/profile", false);
        access(Role.STORE_MANAGER, "GET", "/admin/devices", true);
        access(Role.STORE_MANAGER, "POST", "/admin/devices", false);
        access(Role.STORE_MANAGER, "GET", "/admin/audit", false);
        access(Role.STORE_MANAGER, "GET", "/sales", true);
        access(Role.STORE_MANAGER, "GET", "/unknown/page", false);

        access(Role.AUDITOR, "GET", "/admin/audit", true);
        access(Role.AUDITOR, "GET", "/alerts", true);
        access(Role.AUDITOR, "POST", "/alerts", true);
        access(Role.AUDITOR, "GET", "/reports/shifts", true);
        access(Role.AUDITOR, "GET", "/admin/channel-commissions", true);
        access(Role.AUDITOR, "POST", "/admin/channel-commissions", false);
        access(Role.AUDITOR, "GET", "/transactions", true);
        access(Role.AUDITOR, "GET", "/orders/new", false);
        access(Role.AUDITOR, "GET", "/admin/users", false);
        access(Role.AUDITOR, "GET", "/admin/users/edit", false);
        access(Role.AUDITOR, "GET", "/tax", false);

        access(Role.TECHNICIAN, "GET", "/admin/devices", true);
        access(Role.TECHNICIAN, "POST", "/admin/devices", true);
        access(Role.TECHNICIAN, "GET", "/home", true);
        access(Role.TECHNICIAN, "GET", "/orders/new", false);
        access(Role.TECHNICIAN, "GET", "/transactions", false);
        access(Role.TECHNICIAN, "GET", "/admin/audit", false);

        access(Role.AFFILIATE, "GET", "/affiliate/portal", true);
        access(Role.AFFILIATE, "POST", "/affiliate/portal", true);
        access(Role.AFFILIATE, "GET", "/home", true);
        access(Role.AFFILIATE, "GET", "/orders/new", false);
        access(Role.AFFILIATE, "GET", "/transactions", false);
        access(Role.AFFILIATE, "GET", "/shift", false);
        access(Role.AFFILIATE, "GET", "/admin/affiliates", false);
        access(Role.AFFILIATE, "GET", "/tax", false);

        for (Role r : Role.values()) {
            access(r, "GET", "/profile", true);
            access(r, "GET", "/guide", true);
            access(r, "GET", "/guide/article", true);
            access(r, "GET", "/logout", true);
        }

        check("HEAD is treated as a read", AccessPolicy.requiredFor("/admin/taxes", "HEAD"), AccessPolicy.requiredFor("/admin/taxes", "GET"));
        check("POST on /admin/taxes needs TAX_MANAGE", AccessPolicy.requiredFor("/admin/taxes", "POST"), Permission.TAX_MANAGE);
        check("unknown path fails closed", AccessPolicy.requiredFor("/never/heard/of/it", "GET"), Permission.SYSTEM_ADMIN);
        check("a path that only starts like a prefix fails closed", AccessPolicy.requiredFor("/ordersx", "GET"), Permission.SYSTEM_ADMIN);

        HttpSession s = session();
        String token = CsrfToken.ensure(s);
        check("token is 64 hex characters", token != null && token.matches("[0-9a-f]{64}"), true);
        check("token is stable inside one session", CsrfToken.ensure(s), token);
        check("another session gets another token", CsrfToken.ensure(session()).equals(token), false);
        check("GET is safe", CsrfToken.isUnsafe("GET"), false);
        check("HEAD is safe", CsrfToken.isUnsafe("HEAD"), false);
        check("OPTIONS is safe", CsrfToken.isUnsafe("OPTIONS"), false);
        check("POST is unsafe", CsrfToken.isUnsafe("POST"), true);
        check("PUT is unsafe", CsrfToken.isUnsafe("PUT"), true);
        check("DELETE is unsafe", CsrfToken.isUnsafe("DELETE"), true);
        check("form field with the right token passes", CsrfToken.matches(request(null, token), token), true);
        check("header with the right token passes", CsrfToken.matches(request(token, null), token), true);
        check("empty header falls back to the form field", CsrfToken.matches(request("", token), token), true);
        check("wrong token is rejected", CsrfToken.matches(request(null, "0000"), token), false);
        check("token of the same length but different is rejected", CsrfToken.matches(request(null, token.replace(token.charAt(0), token.charAt(0) == 'a' ? 'b' : 'a')), token), false);
        check("missing token is rejected", CsrfToken.matches(request(null, null), token), false);
        check("missing session token is rejected", CsrfToken.matches(request(token, null), null), false);
        check("a wrong header is not rescued by a right field", CsrfToken.matches(request("bad", token), token), false);

        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}
