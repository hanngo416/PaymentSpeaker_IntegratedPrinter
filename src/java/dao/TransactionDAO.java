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
import model.view.TxRow;
import rules.Verdict;
import util.DBUtil;

public class TransactionDAO {

    private static final String INSERT_TRANSACTION
            = "INSERT INTO Transactions "
            + "(bank_trans_id, order_id, device_id, amount, content, bank_account, gateway, status, trans_time) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ISNULL(?, SYSDATETIME()))";

    private static final String MARK_ORDER_PAID
            = "UPDATE Orders SET status = N'PAID' WHERE order_id = ? AND status = N'PENDING'";

    private static final String INSERT_ALERT
            = "INSERT INTO FraudAlerts (trans_id, rule_code, reason, severity) VALUES (?, ?, ?, ?)";

    private static final String COUNT_SIMILAR
            = "SELECT COUNT(*) FROM Transactions "
            + "WHERE amount = ? AND content = ? "
            + "AND received_at >= DATEADD(SECOND, -?, SYSDATETIME())";

    private static final String COUNT_RECENT
            = "SELECT COUNT(*) FROM Transactions "
            + "WHERE trans_time > ? AND trans_time <= ?";

    private static final String EXISTS_BANK_ID
            = "SELECT 1 FROM Transactions WHERE bank_trans_id = ?";

    private static final String FROM
            = " FROM Transactions t "
            + "LEFT JOIN Orders o ON o.order_id = t.order_id "
            + "LEFT JOIN Devices d ON d.device_id = t.device_id";

    public int countSimilarRecent(BigDecimal amount, String content, int seconds)
            throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(COUNT_SIMILAR)) {
            ps.setBigDecimal(1, amount);
            ps.setString(2, content);
            ps.setInt(3, seconds);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
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
                if (verdict.isLegit() && t.getOrderId() != null) {
                    try (PreparedStatement ps = con.prepareStatement(MARK_ORDER_PAID)) {
                        ps.setInt(1, t.getOrderId());
                        if (ps.executeUpdate() == 0) {
                            finalVerdict = Verdict.flag(TxLabel.AMOUNT_MISMATCH, "ORDER_NOT_PENDING",
                                    "The order changed state before the payment was recorded.", Verdict.MEDIUM);
                        }
                    }
                }
                int transId = insertTransaction(con, t, finalVerdict.getLabel());
                if (!finalVerdict.isLegit()) {
                    try (PreparedStatement ps = con.prepareStatement(INSERT_ALERT)) {
                        ps.setInt(1, transId);
                        ps.setString(2, finalVerdict.getRuleCode());
                        ps.setString(3, finalVerdict.getReason());
                        ps.setString(4, finalVerdict.getSeverity());
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return finalVerdict;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
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
            String sql = "SELECT t.trans_id, t.bank_trans_id, o.order_code, d.device_code, t.amount, "
                    + "t.content, t.status, t.trans_time" + FROM + where
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
                        r.setDeviceCode(rs.getString("device_code"));
                        r.setAmount(rs.getBigDecimal("amount"));
                        r.setContent(rs.getString("content"));
                        r.setStatus(rs.getString("status"));
                        r.setTransTime(rs.getTimestamp("trans_time"));
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
                + "SUM(CASE WHEN t.status = N'LEGIT' THEN t.amount ELSE 0 END) AS legit_total, "
                + "SUM(CASE WHEN t.status <> N'LEGIT' THEN t.amount ELSE 0 END) AS flagged_total, "
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

    private int insertTransaction(Connection con, Transaction t, TxLabel label) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_TRANSACTION, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getBankTransId());
            setInteger(ps, 2, t.getOrderId());
            setInteger(ps, 3, t.getDeviceId());
            ps.setBigDecimal(4, t.getAmount());
            ps.setString(5, t.getContent());
            ps.setString(6, t.getBankAccount());
            ps.setString(7, t.getGateway());
            ps.setString(8, label.name());
            if (t.getTransTime() == null) {
                ps.setNull(9, Types.TIMESTAMP);
            } else {
                ps.setTimestamp(9, t.getTransTime());
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
        if (f.getDeviceId() != null) {
            w.append(" AND t.device_id = ?");
            params.add(f.getDeviceId());
        }
        if (f.getLabel() != null) {
            w.append(" AND t.status = ?");
            params.add(f.getLabel());
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
