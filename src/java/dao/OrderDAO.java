/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.Order;
import model.enums.OrderStatus;
import util.DBUtil;

/**
 *
 * @author PC
 */
public class OrderDAO {

    private static final String SELECT_COLUMNS
            = "SELECT order_id, order_code, created_by, created_by_code, amount, ISNULL(cash_amount, 0) AS cash_amount, discount_code, ISNULL(discount_amount, 0) AS discount_amount, status, qr_content, "
            + "expires_at, created_at, DATEDIFF(SECOND, GETDATE(), expires_at) AS remaining_seconds "
            + "FROM Orders ";

    // A cancelled or overdue order gives its discount code use back
    private static final String RELEASE_PREFIX = "DECLARE @c TABLE (code NVARCHAR(30)); "
            + "UPDATE Orders SET status = N'CANCELLED' OUTPUT inserted.discount_code INTO @c ";
    private static final String RELEASE_SUFFIX = "; UPDATE d SET used_count = d.used_count - (SELECT COUNT(*) FROM @c WHERE code = d.code) "
            + "FROM DiscountCodes d WHERE d.code IN (SELECT code FROM @c)";

    public void insert(Order order, int validMinutes) throws SQLException {
        String sql = "INSERT INTO Orders (order_code, created_by, created_by_code, amount, cash_amount, discount_code, discount_amount, status, qr_content, expires_at) "
                + "VALUES (?, ?, (SELECT user_code FROM Users WHERE user_id = ?), ?, ?, ?, ?, ?, ?, DATEADD(MINUTE, ?, GETDATE()))";
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (order.getDiscountCode() != null && !DiscountDAO.consume(con, order.getDiscountCode())) {
                    throw new IllegalArgumentException("This discount code has just run out or expired.");
                }
                ps.setString(1, order.getOrderCode());
                ps.setInt(2, order.getCreatedBy());
                ps.setInt(3, order.getCreatedBy());
                ps.setBigDecimal(4, order.getAmount());
                ps.setBigDecimal(5, order.getCashAmount());
                ps.setString(6, order.getDiscountCode());
                ps.setBigDecimal(7, order.getDiscountAmount());
                ps.setString(8, order.getStatus() != null ? order.getStatus().name() : OrderStatus.PENDING.name());
                ps.setString(9, order.getQrContent());
                ps.setInt(10, validMinutes);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        order.setOrderId(keys.getInt(1));
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public Order findByCode(String orderCode) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_COLUMNS + "WHERE order_code = ?")) {
            ps.setString(1, orderCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Order> findRecent(int limit, Integer createdBy) throws SQLException {
        String sql = "SELECT TOP (?) order_id, order_code, created_by, created_by_code, amount, ISNULL(cash_amount, 0) AS cash_amount, discount_code, ISNULL(discount_amount, 0) AS discount_amount, status, qr_content, "
                + "expires_at, created_at, DATEDIFF(SECOND, GETDATE(), expires_at) AS remaining_seconds "
                + "FROM Orders " + (createdBy == null ? "" : "WHERE created_by = ? ")
                + "ORDER BY created_at DESC, order_id DESC";
        List<Order> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            if (createdBy != null) {
                ps.setInt(2, createdBy);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    // Mark expired orders
    public void expireOverdue() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement()) {
            st.execute(RELEASE_PREFIX + "WHERE status = N'PENDING' AND expires_at < GETDATE()" + RELEASE_SUFFIX);
        }
    }

    // Cancel order
    public boolean cancel(String orderCode) throws SQLException {
        String sql = RELEASE_PREFIX + "WHERE order_code = ? AND status = N'PENDING'" + RELEASE_SUFFIX;
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, orderCode);
            return ps.executeUpdate() == 1;
        }
    }

    private Order map(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setOrderCode(rs.getString("order_code"));
        o.setCreatedBy(rs.getInt("created_by"));
        o.setCreatedByCode(rs.getString("created_by_code"));
        BigDecimal amount = rs.getBigDecimal("amount");
        o.setAmount(amount);
        try {
            o.setCashAmount(rs.getBigDecimal("cash_amount"));
        } catch (SQLException ignored) { }
        o.setDiscountCode(rs.getString("discount_code"));
        o.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setQrContent(rs.getString("qr_content"));
        o.setExpiresAt(rs.getTimestamp("expires_at"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        o.setRemainingSeconds(rs.getInt("remaining_seconds"));
        return o;
    }
}
