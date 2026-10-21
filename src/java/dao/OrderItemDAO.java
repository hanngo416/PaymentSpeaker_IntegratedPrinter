package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.OrderItem;
import util.DBUtil;

public class OrderItemDAO {

    public List<OrderItem> findByOrderId(int orderId) throws SQLException {
        String sql = "SELECT item_id, order_id, product_id, product_name, unit_price, quantity, subtotal "
                + "FROM OrderItems WHERE order_id = ? ORDER BY item_id ASC";
        List<OrderItem> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem item = new OrderItem();
                    item.setItemId(rs.getInt("item_id"));
                    item.setOrderId(rs.getInt("order_id"));
                    int pId = rs.getInt("product_id");
                    item.setProductId(rs.wasNull() ? null : pId);
                    item.setProductName(rs.getString("product_name"));
                    item.setUnitPrice(rs.getBigDecimal("unit_price"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setSubtotal(rs.getBigDecimal("subtotal"));
                    list.add(item);
                }
            }
        }
        return list;
    }

    public static void insertBatch(Connection con, int orderId, List<OrderItem> items) throws SQLException {
        if (items == null || items.isEmpty()) {
            return;
        }
        String sql = "INSERT INTO OrderItems (order_id, product_id, product_name, unit_price, quantity, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (OrderItem item : items) {
                ps.setInt(1, orderId);
                if (item.getProductId() == null) {
                    ps.setNull(2, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(2, item.getProductId());
                }
                ps.setString(3, item.getProductName());
                ps.setBigDecimal(4, item.getUnitPrice());
                ps.setInt(5, item.getQuantity());
                ps.setBigDecimal(6, item.getSubtotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
