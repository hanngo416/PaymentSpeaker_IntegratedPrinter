package util;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

public final class StoreContext {

    public static final int NO_STORE = -1;

    private static final ThreadLocal<int[]> CURRENT = new ThreadLocal<>();

    private StoreContext() {
    }

    public static void set(Integer storeId, boolean platformAdmin) {
        CURRENT.set(new int[]{storeId == null ? NO_STORE : storeId, platformAdmin ? 1 : 0});
    }

    public static void clear() {
        CURRENT.remove();
    }

    static void apply(Connection con) throws SQLException {
        int[] c = CURRENT.get();
        if (c == null) {
            return;
        }
        try (CallableStatement cs = con.prepareCall("{call sp_set_session_context(?, ?)}")) {
            cs.setString(1, "store_id");
            cs.setInt(2, c[0]);
            cs.execute();
        }
        try (CallableStatement cs = con.prepareCall("{call sp_set_session_context(?, ?)}")) {
            cs.setString(1, "is_admin");
            cs.setInt(2, c[1]);
            cs.execute();
        }
    }
}
