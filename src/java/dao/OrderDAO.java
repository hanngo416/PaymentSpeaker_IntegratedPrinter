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
            = "SELECT order_id, order_code, created_by, created_by_code, amount, status, qr_content, "
            + "expires_at, created_at, DATEDIFF(SECOND, GETDATE(), expires_at) AS remaining_seconds "
            + "FROM Orders ";

    public void insert(Order order, int validMinutes) throws SQLException {
        String sql = "INSERT INTO Orders (order_code, created_by, created_by_code, amount, status, qr_content, expires_at) "
                + "VALUES (?, ?, (SELECT user_code FROM Users WHERE user_id = ?), ?, ?, ?, DATEADD(MINUTE, ?, GETDATE()))";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.getOrderCode());
            ps.setInt(2, order.getCreatedBy());
            ps.setInt(3, order.getCreatedBy());
            ps.setBigDecimal(4, order.getAmount());
            ps.setString(5, OrderStatus.PENDING.name());
            ps.setString(6, order.getQrContent());
            ps.setInt(7, validMinutes);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setOrderId(keys.getInt(1));
                }
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
        String sql = "SELECT TOP (?) order_id, order_code, created_by, created_by_code, amount, status, qr_content, "
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

    public int expireOverdue() throws SQLException {
        String sql = "UPDATE Orders SET status = 'EXPIRED' WHERE status = 'PENDING' AND expires_at < GETDATE()";
        try (Connection con = DBUtil.getConnection();
             Statement st = con.createStatement()) {
            return st.executeUpdate(sql);
        }
    }

    public boolean cancel(String orderCode) throws SQLException {
        String sql = "UPDATE Orders SET status = 'CANCELLED' WHERE order_code = ? AND status = 'PENDING'";
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
        o.setStatus(OrderStatus.valueOf(rs.getString("status")));
        o.setQrContent(rs.getString("qr_content"));
        o.setExpiresAt(rs.getTimestamp("expires_at"));
        o.setCreatedAt(rs.getTimestamp("created_at"));
        o.setRemainingSeconds(rs.getInt("remaining_seconds"));
        return o;
    }
}
