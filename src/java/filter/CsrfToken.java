package filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class CsrfToken {

    public static final String SESSION_KEY = "csrfToken";
    public static final String PARAM = "_csrf";
    public static final String HEADER = "X-CSRF-Token";

    private static final SecureRandom RANDOM = new SecureRandom();

    private CsrfToken() {
    }

    public static String ensure(HttpSession session) {
        Object current = session.getAttribute(SESSION_KEY);
        if (current instanceof String) {
            return (String) current;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(64);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        String token = hex.toString();
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    public static boolean isUnsafe(String method) {
        return !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method));
    }

    public static boolean matches(HttpServletRequest request, String expected) {
        String sent = request.getHeader(HEADER);
        if (sent == null || sent.isEmpty()) {
            sent = request.getParameter(PARAM);
        }
        if (sent == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(sent.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
