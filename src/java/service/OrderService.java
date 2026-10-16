/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.OrderDAO;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.List;
import model.Order;
import model.User;
import model.enums.OrderStatus;
import model.enums.Role;
import util.AppConfig;
import util.SqlErrors;
import util.VietQrUtil;

/**
 *
 * @author PC
 */
public class OrderService {

    private static final int MAX_CODE_ATTEMPTS = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderDAO orderDAO = new OrderDAO();

    public Order create(User user, String rawAmount) throws SQLException {
        return create(user, rawAmount, null);
    }

    public Order create(User user, String rawAmount, String rawCashAmount) throws SQLException {
        long amount = parseAmount(rawAmount);
        long cash = parseOptionalAmount(rawCashAmount);

        if (cash < 0) {
            throw new IllegalArgumentException("Cash amount cannot be negative.");
        }
        if (cash > amount) {
            throw new IllegalArgumentException("Cash amount cannot exceed order total.");
        }
        boolean isAllCash = (cash == amount);
        long transfer = isAllCash ? 0 : (amount - cash);

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            String code = AppConfig.ORDER_PREFIX + String.format("%05d", RANDOM.nextInt(100000));

            Order order = new Order();
            order.setOrderCode(code);
            order.setCreatedBy(user.getUserId());
            order.setAmount(BigDecimal.valueOf(amount));
            order.setCashAmount(BigDecimal.valueOf(cash));
            order.setStatus(isAllCash ? OrderStatus.COMPLETED : OrderStatus.PENDING);
            order.setQrContent(isAllCash ? null : VietQrUtil.buildImageUrl(transfer, code));
            try {
                orderDAO.insert(order, AppConfig.ORDER_MINUTES);
                Order created = orderDAO.findByCode(code);
                AuditService.log(user, "CREATE_ORDER", "ORDER", code,
                        "amount", String.valueOf(amount),
                        "cash", String.valueOf(cash),
                        "transfer", String.valueOf(transfer),
                        "status", order.getStatus().name());
                return created;
            } catch (SQLException e) {
                if (!SqlErrors.isDuplicateKey(e)) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Could not generate a unique order code. Please try again.");
    }

    public Order findForUser(User user, String code) throws SQLException {
        orderDAO.expireOverdue();
        Order order = orderDAO.findByCode(code);
        if (order != null && !isOwnerOrAdmin(user, order)) {
            return null;
        }
        return order;
    }

    public boolean cancel(User user, String code) throws SQLException {
        Order order = findForUser(user, code);
        boolean cancelled = order != null && orderDAO.cancel(code);
        if (cancelled) {
            AuditService.log(user, "CANCEL_ORDER", "ORDER", code, "cash", order.getCashAmount().toPlainString());
        }
        return cancelled;
    }

    public List<Order> recent(User user) throws SQLException {
        orderDAO.expireOverdue();
        return orderDAO.findRecent(10, user.getRole() == Role.ADMIN ? null : user.getUserId());
    }

    private static boolean isOwnerOrAdmin(User user, Order order) {
        return user.getRole() == Role.ADMIN || order.getCreatedBy() == user.getUserId();
    }

    static long parseAmount(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Please enter an amount.");
        }
        String digits = raw.replaceAll("[.,\\s]", "");
        if (!digits.matches("\\d{1,12}")) {
            throw new IllegalArgumentException("The amount must contain digits only.");
        }
        long amount = Long.parseLong(digits);
        if (amount < AppConfig.MIN_AMOUNT || amount > AppConfig.MAX_AMOUNT) {
            throw new IllegalArgumentException("The amount must be between " + AppConfig.MIN_AMOUNT
                    + " and " + AppConfig.MAX_AMOUNT + " VND.");
        }
        return amount;
    }

    static long parseOptionalAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        String digits = raw.replaceAll("[.,\\s]", "");
        if (!digits.matches("\\d{1,12}")) {
            throw new IllegalArgumentException("Cash amount must contain digits only.");
        }
        return Long.parseLong(digits);
    }
}
