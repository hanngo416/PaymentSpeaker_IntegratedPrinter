/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import dao.DeviceDAO;
import dao.OrderDAO;
import dao.TransactionDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.Order;
import model.Transaction;
import rules.RuleConfig;
import rules.RuleContext;
import rules.RulesEngine;
import rules.Verdict;
import util.AppConfig;
import util.SqlErrors;

/**
 *
 * @author PC
 */
public class TransactionIngestService {

    public enum Outcome { CREATED, DUPLICATE, IGNORED }

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("999999999999999");

    private static final Pattern ORDER_CODE
            = Pattern.compile("(?i)" + Pattern.quote(AppConfig.ORDER_PREFIX) + "(\\d{5})(?!\\d)");

    private final TransactionDAO transactionDAO = new TransactionDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final DeviceDAO deviceDAO = new DeviceDAO();
    private final RulesEngine engine = RulesEngine.standard();

    public Outcome ingest(Map<String, String> payload) throws SQLException {
        String bankTransId = trimToNull(payload.get("id"));
        if (bankTransId == null || bankTransId.length() > 100) {
            throw new IllegalArgumentException("Missing or invalid field: id");
        }

        if (!"in".equalsIgnoreCase(trimToNull(payload.get("transferType")))) {
            return Outcome.IGNORED;
        }
        if (transactionDAO.existsByBankTransId(bankTransId)) {
            return Outcome.DUPLICATE;
        }
        orderDAO.expireOverdue();

        Transaction t = new Transaction();
        t.setBankTransId(bankTransId);
        t.setAmount(parseAmount(payload.get("transferAmount")));

        String rawContent = trimToNull(payload.get("content"));
        String content = truncate(rawContent == null ? "" : rawContent, 500);
        t.setContent(content);
        t.setBankAccount(truncate(trimToNull(payload.get("accountNumber")), 50));
        String gateway = trimToNull(payload.get("gateway"));
        t.setGateway(truncate(gateway == null ? "SEPAY" : gateway, 50));
        t.setTransTime(parseTime(payload.get("transactionDate")));

        t.setDeviceId(deviceDAO.findSpeakerId());
        Order order = findOrder(content);
        if (order != null) {
            t.setOrderId(order.getOrderId());
        }

        Timestamp when = t.getTransTime() != null ? t.getTransTime() : new Timestamp(System.currentTimeMillis());
        int similar = transactionDAO.countSimilarRecent(
                t.getAmount(), t.getContent(), RuleConfig.DUPLICATE_WINDOW_SECONDS);
        int recent = transactionDAO.countRecent(when, RuleConfig.BURST_WINDOW_SECONDS);
        Verdict verdict = engine.evaluate(new RuleContext(t, order, similar, recent));

        try {
            transactionDAO.record(t, verdict);
            return Outcome.CREATED;
        } catch (SQLException e) {
            if (SqlErrors.isDuplicateKey(e)) {
                return Outcome.DUPLICATE;
            }
            throw e;
        }
    }

    private Order findOrder(String content) throws SQLException {
        Matcher m = ORDER_CODE.matcher(content);
        if (!m.find()) {
            return null;
        }
        return orderDAO.findByCode(AppConfig.ORDER_PREFIX + m.group(1));
    }

    private static BigDecimal parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing field: transferAmount");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("transferAmount is not a number");
        }
        if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("transferAmount is out of the allowed range");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static Timestamp parseTime(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Timestamp.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
