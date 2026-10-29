package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.view.StockRow;
import util.DBUtil;

public class StockDAO {

    public static final String SALE = "SALE";
    public static final String RESTOCK = "RESTOCK";
    public static final String ADJUST = "ADJUST";
    public static final String INITIAL = "INITIAL";

    private static final String SOLD_LINES
            = "(SELECT product_id, SUM(quantity) AS qty FROM OrderItems WHERE order_id = ? AND product_id IS NOT NULL GROUP BY product_id) s ";

    private static final String MOVEMENT_FROM
            = "FROM StockMovements m JOIN Products p ON p.product_id = m.product_id "
            + "LEFT JOIN Orders o ON o.order_id = m.order_id LEFT JOIN Users u ON u.user_id = m.created_by ";

    public static void deductForOrder(Connection con, int orderId) throws SQLException {
        String insert = "INSERT INTO StockMovements (product_id, quantity_change, quantity_after, reason, order_id, created_by) "
                + "SELECT s.product_id, -s.qty, p.stock_quantity - s.qty, N'SALE', o.order_id, o.created_by FROM " + SOLD_LINES
                + "JOIN Products p WITH (UPDLOCK) ON p.product_id = s.product_id JOIN Orders o ON o.order_id = ?";
        String update = "UPDATE p SET p.stock_quantity = p.stock_quantity - s.qty FROM Products p JOIN " + SOLD_LINES
                + "ON s.product_id = p.product_id";
        try (PreparedStatement ps = con.prepareStatement(insert)) {
            ps.setInt(1, orderId);
            ps.setInt(2, orderId);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(update)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }

    public static void recordInitial(Connection con, int productId, int quantity) throws SQLException {
        if (quantity == 0) {
            return;
        }
        insertMovement(con, productId, quantity, quantity, INITIAL, "Opening balance", null);
    }

    public boolean restock(int productId, int quantity, Integer userId, String note) throws SQLException {
        return change(productId, RESTOCK, quantity, userId, note);
    }

    public boolean adjust(int productId, int newQuantity, Integer userId, String note) throws SQLException {
        return change(productId, ADJUST, newQuantity, userId, note);
    }

    private boolean change(int productId, String reason, int value, Integer userId, String note) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                int current;
                try (PreparedStatement ps = con.prepareStatement("SELECT stock_quantity FROM Products WITH (UPDLOCK) WHERE product_id = ?")) {
                    ps.setInt(1, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                        current = rs.getInt(1);
                    }
                }
                int after = RESTOCK.equals(reason) ? current + value : value;
                try (PreparedStatement ps = con.prepareStatement("UPDATE Products SET stock_quantity = ? WHERE product_id = ?")) {
                    ps.setInt(1, after);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }
                insertMovement(con, productId, after - current, after, reason, note, userId);
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    private static void insertMovement(Connection con, int productId, int change, int after, String reason, String note, Integer userId)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO StockMovements (product_id, quantity_change, quantity_after, reason, note, created_by) VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setInt(1, productId);
            ps.setInt(2, change);
            ps.setInt(3, after);
            ps.setString(4, reason);
            ps.setString(5, note);
            if (userId == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, userId);
            }
            ps.executeUpdate();
        }
    }

    public int countLowStock(int threshold) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM Products WHERE active = 1 AND stock_quantity <= ?")) {
            ps.setInt(1, threshold);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Page<StockRow> search(Integer productId, String reason, java.sql.Date from, java.sql.Date to, int page, int pageSize)
            throws SQLException {
        String where = "WHERE (? IS NULL OR m.product_id = ?) AND (? IS NULL OR m.reason = ?) "
                + "AND (? IS NULL OR m.created_at >= ?) AND (? IS NULL OR m.created_at < DATEADD(DAY, 1, ?)) ";
        try (Connection con = DBUtil.getConnection()) {
            int total;
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) " + MOVEMENT_FROM + where)) {
                bind(ps, productId, reason, from, to, 1);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            List<StockRow> items = new ArrayList<>();
            String sql = "SELECT m.movement_id, m.product_id, p.product_code, p.product_name, m.quantity_change, m.quantity_after, "
                    + "m.reason, o.order_code, m.note, u.user_code, m.created_at " + MOVEMENT_FROM + where
                    + "ORDER BY m.movement_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int i = bind(ps, productId, reason, from, to, 1);
                ps.setInt(i++, Math.max(0, (page - 1) * pageSize));
                ps.setInt(i, pageSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        StockRow r = new StockRow();
                        r.setMovementId(rs.getInt("movement_id"));
                        r.setProductId(rs.getInt("product_id"));
                        r.setProductCode(rs.getString("product_code"));
                        r.setProductName(rs.getString("product_name"));
                        r.setQuantityChange(rs.getInt("quantity_change"));
                        r.setQuantityAfter(rs.getInt("quantity_after"));
                        r.setReason(rs.getString("reason"));
                        r.setOrderCode(rs.getString("order_code"));
                        r.setNote(rs.getString("note"));
                        r.setCreatedByCode(rs.getString("user_code"));
                        r.setCreatedAt(rs.getTimestamp("created_at"));
                        items.add(r);
                    }
                }
            }
            return new Page<>(items, total, page, pageSize);
        }
    }

    private static int bind(PreparedStatement ps, Integer productId, String reason, java.sql.Date from, java.sql.Date to, int i)
            throws SQLException {
        ps.setObject(i++, productId, Types.INTEGER);
        ps.setObject(i++, productId, Types.INTEGER);
        ps.setString(i++, reason);
        ps.setString(i++, reason);
        ps.setDate(i++, from);
        ps.setDate(i++, from);
        ps.setDate(i++, to);
        ps.setDate(i++, to);
        return i;
    }
}
