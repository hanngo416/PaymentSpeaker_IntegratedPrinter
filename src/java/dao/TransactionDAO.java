package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.Page;
import model.Transaction;
import model.TxFilter;
import model.enums.TxLabel;
import model.view.DayTotal;
import model.view.ReceiptRow;
import model.view.SpeakItem;
import model.view.TxRow;
import rules.Verdict;
import util.DBUtil;

public class TransactionDAO {

    private static final String INSERT_TRANSACTION
            = "INSERT INTO Transactions "
            + "(bank_trans_id, order_id, amount, content, bank_account, gateway, status, trans_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ISNULL(?, SYSDATETIME()))";

    private static final String CREDIT_ORDER
            = "UPDATE Orders SET status = N'COMPLETED' WHERE order_id = ? AND status IN (N'PENDING', N'EXPIRED')";

    private static final String INSERT_ALERT
            = "INSERT INTO FraudAlerts (trans_id, rule_code, reason, severity) VALUES (?, ?, ?, ?)";

    private static final String COUNT_SIMILAR
            = "SELECT COUNT(*) FROM Transactions "
            + "WHERE amount = ? AND content = ? "
            + "AND trans_time BETWEEN ? AND ?";

    private static final String COUNT_RECENT
            = "SELECT COUNT(*) FROM Transactions "
            + "WHERE trans_time > ? AND trans_time <= ?";

    private static final String EXISTS_BANK_ID
            = "SELECT 1 FROM Transactions WHERE bank_trans_id = ?";

    private static final String SELECT_TO_ANNOUNCE
            = "SELECT TOP (?) t.trans_id, t.amount, t.trans_time, o.order_code FROM Transactions t "
            + "LEFT JOIN Orders o ON o.order_id = t.order_id "
            + "WHERE t.status IN (N'COMPLETED', N'LEGIT') AND t.announced = 0 "
            + "AND t.received_at >= DATEADD(MINUTE, -?, SYSDATETIME()) "
            + "AND t.trans_time >= DATEADD(MINUTE, -?, SYSDATETIME()) "
            + "ORDER BY t.trans_id";

    private static final String MARK_ANNOUNCED
            = "UPDATE Transactions SET announced = 1 WHERE trans_id = ? AND announced = 0";

    private static final String FROM
            = " FROM Transactions t "
            + "LEFT JOIN Orders o ON o.order_id = t.order_id";

    public int countSimilarRecent(BigDecimal amount, String content, Timestamp at, int seconds)
            throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_SIMILAR)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, content);
            ps.setTimestamp(3, new Timestamp(at.getTime() - seconds * 1000L));
            ps.setTimestamp(4, new Timestamp(at.getTime() + seconds * 1000L));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public Timestamp findLatestTransTime() throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT MAX(trans_time) FROM Transactions");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getTimestamp(1) : null;
        }
    }

    public int countRecent(Timestamp at, int seconds) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_RECENT)) {
            ps.setTimestamp(1, new Timestamp(at.getTime() - seconds * 1000L));
            ps.setTimestamp(2, at);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public boolean existsByBankTransId(String bankTransId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(EXISTS_BANK_ID)) {
            ps.setString(1, bankTransId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Verdict record(Transaction t, Verdict verdict) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                Verdict finalVerdict = verdict;
                if (verdict.getLabel() == TxLabel.LEGIT && t.getOrderId() != null) {
                    try (PreparedStatement ps = con.prepareStatement(CREDIT_ORDER)) {
                        ps.setInt(1, t.getOrderId());
                        if (ps.executeUpdate() == 0) {
                            finalVerdict = Verdict.flag(TxLabel.ORDER_CLOSED, "ORDER_NOT_PENDING",
                                    "The order changed state before the payment was recorded.", Verdict.MEDIUM)
                                    .withExtras(verdict.getExtras());
                        } else {
                            StockDAO.deductForOrder(con, t.getOrderId());
                        }
                    }
                }
                int transId = insertTransaction(con, t, finalVerdict.getLabel());
                if (finalVerdict.needsAlert()) {
                    insertAlert(con, transId, finalVerdict);
                }
                for (Verdict extra : finalVerdict.getExtras()) {
                    insertAlert(con, transId, extra);
                }
                con.commit();
                return finalVerdict;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<SpeakItem> findToAnnounce(int limit, int maxAgeMinutes, int maxBankAgeMinutes) throws SQLException {
        List<SpeakItem> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT_TO_ANNOUNCE)) {
            ps.setInt(1, limit);
            ps.setInt(2, maxAgeMinutes);
            ps.setInt(3, maxBankAgeMinutes);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SpeakItem(rs.getInt("trans_id"), rs.getBigDecimal("amount").longValue(),
                            rs.getString("order_code"), rs.getTimestamp("trans_time")));
                }
            }
        }
        return list;
    }

    public boolean markAnnounced(int transId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(MARK_ANNOUNCED)) {
            ps.setInt(1, transId);
            return ps.executeUpdate() > 0;
        }
    }

    private static final String RECEIPT_SELECT
            = "SELECT t.trans_id, t.bank_trans_id, o.order_id, o.order_code, o.created_by_code, t.amount, o.amount AS order_amount, "
            + "ISNULL(o.cash_amount, 0) AS cash_amount, o.vat_rate, o.vat_amount, o.vat_included, o.discount_code, ISNULL(o.discount_amount, 0) AS discount_amount, (SELECT referral_code FROM Affiliates WHERE affiliate_id = o.referred_by_affiliate_id) AS affiliate_code, t.trans_time, t.printed FROM Transactions t JOIN Orders o ON o.order_id = t.order_id "
            + "WHERE t.status IN (N'COMPLETED', N'LEGIT') ";

    public ReceiptRow findReceipt(int transId, Integer cashierId) throws SQLException {
        String sql = RECEIPT_SELECT + "AND t.trans_id = ? " + (cashierId == null ? "" : "AND o.created_by = ?");
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transId);
            if (cashierId != null) {
                ps.setInt(2, cashierId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapReceipt(rs) : null;
            }
        }
    }

    public ReceiptRow findReceiptByOrder(int orderId, Integer cashierId) throws SQLException {
        String sql = "SELECT ISNULL(t.trans_id, 0) AS trans_id, t.bank_trans_id, o.order_id, o.order_code, o.created_by_code, "
                + "ISNULL(t.amount, 0) AS amount, o.amount AS order_amount, ISNULL(o.cash_amount, 0) AS cash_amount, o.vat_rate, o.vat_amount, o.vat_included, o.discount_code, ISNULL(o.discount_amount, 0) AS discount_amount, (SELECT referral_code FROM Affiliates WHERE affiliate_id = o.referred_by_affiliate_id) AS affiliate_code, "
                + "ISNULL(t.trans_time, o.created_at) AS trans_time, ISNULL(t.printed, 0) AS printed "
                + "FROM Orders o LEFT JOIN Transactions t ON t.order_id = o.order_id AND t.status IN (N'COMPLETED', N'LEGIT') "
                + "WHERE o.order_id = ? AND o.status IN (N'COMPLETED', N'PAID') "
                + (cashierId == null ? "" : "AND o.created_by = ?");
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            if (cashierId != null) {
                ps.setInt(2, cashierId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapReceipt(rs) : null;
            }
        }
    }

    public void markPrinted(int transId) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE Transactions SET printed = 1 WHERE trans_id = ?")) {
            ps.setInt(1, transId);
            ps.executeUpdate();
        }
    }

    private static ReceiptRow mapReceipt(ResultSet rs) throws SQLException {
        ReceiptRow r = new ReceiptRow();
        r.setTransId(rs.getInt("trans_id"));
        r.setOrderId(rs.getInt("order_id"));
        r.setBankTransId(rs.getString("bank_trans_id"));
        r.setOrderCode(rs.getString("order_code"));
        r.setCashierCode(rs.getString("created_by_code"));
        r.setAmount(rs.getBigDecimal("amount"));
        r.setOrderAmount(rs.getBigDecimal("order_amount"));
        r.setCashAmount(DBUtil.optionalDecimal(rs, "cash_amount"));
        r.setVatRate(rs.getBigDecimal("vat_rate"));
        r.setStoredVat(rs.getBigDecimal("vat_amount"));
        r.setVatIncluded(rs.getBoolean("vat_included"));
        r.setDiscountCode(rs.getString("discount_code"));
        r.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        r.setAffiliateCode(rs.getString("affiliate_code"));
        r.setTransTime(rs.getTimestamp("trans_time"));
        r.setPrinted(rs.getBoolean("printed"));
        return r;
    }

    public Page<TxRow> search(TxFilter f) throws SQLException {
        List<Object> params = new ArrayList<>();
        String where = where(f, params);
        int total;
        List<TxRow> items = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*)" + FROM + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            String sql = "SELECT t.trans_id, t.bank_trans_id, o.order_code, t.amount, "
                    + "t.content, t.status, t.trans_time, o.created_by" + FROM + where
                    + " ORDER BY t.trans_time DESC, t.trans_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, params);
                ps.setInt(params.size() + 1, f.getOffset());
                ps.setInt(params.size() + 2, f.getPageSize());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TxRow r = new TxRow();
                        r.setTransId(rs.getInt("trans_id"));
                        r.setBankTransId(rs.getString("bank_trans_id"));
                        r.setOrderCode(rs.getString("order_code"));
                        r.setAmount(rs.getBigDecimal("amount"));
                        r.setContent(rs.getString("content"));
                        r.setStatus(rs.getString("status"));
                        r.setTransTime(rs.getTimestamp("trans_time"));
                        Object cb = rs.getObject("created_by");
                        r.setCreatedBy(cb != null ? ((Number) cb).intValue() : null);
                        items.add(r);
                    }
                }
            }
        }
        return new Page<>(items, total, f.getPage(), f.getPageSize());
    }

    public List<DayTotal> dailyTotals(TxFilter f) throws SQLException {
        List<Object> params = new ArrayList<>();
        String where = where(f, params);
        String sql = "SELECT CAST(t.trans_time AS DATE) AS day, "
                + "SUM(CASE WHEN t.status IN (N'COMPLETED', N'LEGIT') THEN t.amount ELSE 0 END) AS legit_total, "
                + "SUM(CASE WHEN t.status NOT IN (N'COMPLETED', N'LEGIT') THEN t.amount ELSE 0 END) AS flagged_total, "
                + "COUNT(*) AS cnt" + FROM + where
                + " GROUP BY CAST(t.trans_time AS DATE) ORDER BY CAST(t.trans_time AS DATE)";
        List<DayTotal> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DayTotal d = new DayTotal();
                    d.setDay(rs.getDate("day"));
                    d.setLegitTotal(rs.getBigDecimal("legit_total"));
                    d.setFlaggedTotal(rs.getBigDecimal("flagged_total"));
                    d.setCount(rs.getInt("cnt"));
                    list.add(d);
                }
            }
        }
        return list;
    }

    private void insertAlert(Connection con, int transId, Verdict v) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_ALERT)) {
            ps.setInt(1, transId);
            ps.setString(2, v.getRuleCode());
            ps.setString(3, v.getReason());
            ps.setString(4, v.getSeverity());
            ps.executeUpdate();
        }
    }

    private int insertTransaction(Connection con, Transaction t, TxLabel label) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_TRANSACTION, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getBankTransId());
            setInteger(ps, 2, t.getOrderId());
            ps.setBigDecimal(3, t.getAmount());
            ps.setString(4, t.getContent());
            ps.setString(5, t.getBankAccount());
            ps.setString(6, t.getGateway());
            ps.setString(7, label == TxLabel.LEGIT ? "COMPLETED" : "CANCELLED");
            if (t.getTransTime() == null) {
                ps.setNull(8, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(8, t.getTransTime());
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static String where(TxFilter f, List<Object> params) {
        StringBuilder w = new StringBuilder(" WHERE 1 = 1");
        if (f.getLabel() != null) {
            if ("COMPLETED".equalsIgnoreCase(f.getLabel()) || "LEGIT".equalsIgnoreCase(f.getLabel())) {
                w.append(" AND t.status IN (N'COMPLETED', N'LEGIT')");
            } else if ("CANCELLED".equalsIgnoreCase(f.getLabel())) {
                w.append(" AND t.status NOT IN (N'COMPLETED', N'LEGIT')");
            } else {
                w.append(" AND t.status = ?");
                params.add(f.getLabel());
            }
        }
        if (f.getFrom() != null) {
            w.append(" AND t.trans_time >= ?");
            params.add(f.getFrom());
        }
        if (f.getTo() != null) {
            w.append(" AND t.trans_time < DATEADD(DAY, 1, CAST(? AS DATETIME2))");
            params.add(f.getTo());
        }
        return w.toString();
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Date) {
                ps.setDate(i + 1, (Date) p);
            } else {
                ps.setObject(i + 1, p);
            }
        }
    }

    private static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
