package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.DBUtil;

public class TaxToolsDAO {

    public static final String[] OBLIGATION_STATUSES = {"NOT_ASSESSED", "NEEDS_INFO", "APPLICABLE", "NOT_APPLICABLE", "PREPARATION",
        "READY_FOR_REVIEW", "SUBMITTED", "AWAITING_PAYMENT", "PAID", "OVERDUE", "NEEDS_ADJUSTMENT"};

    public List<Map<String, Object>> query(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int c = 1; c <= md.getColumnCount(); c++) {
                        row.put(md.getColumnLabel(c), rs.getObject(c));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public int update(String sql, Object... args) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    ps.setNull(i + 1, Types.NULL);
                } else {
                    ps.setObject(i + 1, args[i]);
                }
            }
            return ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> obligations() throws SQLException {
        return query("SELECT o.obligation_id, o.kind, o.period_label, o.due_date, o.status, o.responsible_user, u.full_name AS responsible, "
                + "o.estimated_amount, o.declared_amount, o.confirmed_amount, o.submission_ref, o.completed_on, o.notes, "
                + "ISNULL((SELECT SUM(amount) FROM TaxPayments p WHERE p.obligation_id = o.obligation_id), 0) AS paid_amount, "
                + "CASE WHEN o.status NOT IN ('PAID', 'NOT_APPLICABLE') AND o.due_date < CAST(GETDATE() AS DATE) THEN 1 ELSE 0 END AS late "
                + "FROM TaxObligations o LEFT JOIN Users u ON u.user_id = o.responsible_user ORDER BY o.due_date, o.obligation_id");
    }

    public Map<String, Object> obligation(int id) throws SQLException {
        List<Map<String, Object>> rows = query("SELECT o.*, ISNULL((SELECT SUM(amount) FROM TaxPayments p WHERE p.obligation_id = o.obligation_id), 0) AS paid_amount "
                + "FROM TaxObligations o WHERE o.obligation_id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> history(int obligationId) throws SQLException {
        return query("SELECT h.from_status, h.to_status, h.note, u.full_name AS changed_by, h.changed_at FROM TaxObligationHistory h "
                + "JOIN Users u ON u.user_id = h.changed_by WHERE h.obligation_id = ? ORDER BY h.changed_at DESC, h.history_id DESC", obligationId);
    }

    public void changeObligation(int id, String from, String to, Integer responsible, BigDecimal declared, BigDecimal confirmed,
            String submissionRef, String note, int userId) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("UPDATE TaxObligations SET status = ?, responsible_user = ?, declared_amount = ?, confirmed_amount = ?, "
                        + "submission_ref = ?, completed_on = CASE WHEN ? = 'PAID' THEN CAST(GETDATE() AS DATE) ELSE completed_on END WHERE obligation_id = ?")) {
                    ps.setString(1, to);
                    ps.setObject(2, responsible);
                    ps.setBigDecimal(3, declared);
                    ps.setBigDecimal(4, confirmed);
                    ps.setString(5, submissionRef);
                    ps.setString(6, to);
                    ps.setInt(7, id);
                    ps.executeUpdate();
                }
                if (!from.equals(to)) {
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO TaxObligationHistory (obligation_id, from_status, to_status, note, changed_by) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setInt(1, id);
                        ps.setString(2, from);
                        ps.setString(3, to);
                        ps.setString(4, note);
                        ps.setInt(5, userId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    public List<Map<String, Object>> payments() throws SQLException {
        return query("SELECT p.payment_id, p.obligation_id, o.kind, o.period_label, p.paid_on, p.amount, p.method, p.reference, p.receipt_name, u.full_name AS recorded_by "
                + "FROM TaxPayments p JOIN TaxObligations o ON o.obligation_id = p.obligation_id JOIN Users u ON u.user_id = p.recorded_by ORDER BY p.paid_on DESC, p.payment_id DESC");
    }

    public void addPayment(int obligationId, Date paidOn, BigDecimal amount, String method, String reference, String receipt, int userId) throws SQLException {
        update("INSERT INTO TaxPayments (obligation_id, paid_on, amount, method, reference, receipt_name, recorded_by) VALUES (?, ?, ?, ?, ?, ?, ?)",
                obligationId, paidOn, amount, method, reference, receipt, userId);
    }

    public List<Map<String, Object>> calculations() throws SQLException {
        return query("SELECT c.*, u.full_name AS created_name FROM TaxCalculations c JOIN Users u ON u.user_id = c.created_by ORDER BY c.created_at DESC, c.calc_id DESC");
    }

    public Map<String, Object> calculation(int id) throws SQLException {
        List<Map<String, Object>> rows = query("SELECT * FROM TaxCalculations WHERE calc_id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public void addCalculation(Date calcDate, String period, String sector, BigDecimal revenue, BigDecimal adjustment, BigDecimal vat, BigDecimal pit,
            String stamp, boolean verified, String note, int userId) throws SQLException {
        update("INSERT INTO TaxCalculations (calc_date, period_label, sector, revenue, adjustment, vat_amount, pit_amount, total_amount, rule_stamp, verified_rules, note, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                calcDate, period, sector, revenue, adjustment, vat, pit, vat.add(pit == null ? BigDecimal.ZERO : pit), stamp, verified, note, userId);
    }

    public List<Map<String, Object>> assessments() throws SQLException {
        return query("SELECT a.assessed_on, a.period_label, a.summary, a.verification, u.full_name AS created_name FROM TaxAssessments a "
                + "JOIN Users u ON u.user_id = a.created_by ORDER BY a.created_at DESC, a.assessment_id DESC");
    }

    public void addAssessment(Date on, String period, String summary, String verification, int userId) throws SQLException {
        update("INSERT INTO TaxAssessments (assessed_on, period_label, summary, verification, created_by) VALUES (?, ?, ?, ?, ?)", on, period, summary, verification, userId);
    }

    public List<Map<String, Object>> expenses(String q, String category, String doc, Date from, Date to) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM Expenses WHERE 1 = 1 ");
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isEmpty()) {
            sql.append("AND (supplier LIKE ? OR invoice_no LIKE ?) ");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (category != null && !category.isEmpty()) {
            sql.append("AND category = ? ");
            args.add(category);
        }
        if ("missing".equals(doc)) {
            sql.append("AND has_document = 0 ");
        } else if ("ok".equals(doc)) {
            sql.append("AND has_document = 1 ");
        }
        if (from != null) {
            sql.append("AND spent_on >= ? ");
            args.add(from);
        }
        if (to != null) {
            sql.append("AND spent_on <= ? ");
            args.add(to);
        }
        sql.append("ORDER BY spent_on DESC, expense_id DESC");
        return query(sql.toString(), args.toArray());
    }

    public void addExpense(Date on, String supplier, String taxCode, String category, BigDecimal amount, String method, String invoiceNo, boolean doc, String note, int userId) throws SQLException {
        update("INSERT INTO Expenses (spent_on, supplier, supplier_tax_code, category, amount, payment_method, invoice_no, has_document, note, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                on, supplier, taxCode, category, amount, method, invoiceNo, doc, note, userId);
    }

    public boolean setExpenseTreatment(int id, String treatment) throws SQLException {
        return update("UPDATE Expenses SET tax_treatment = ? WHERE expense_id = ?", treatment, id) == 1;
    }

    public boolean setExpenseDocument(int id, boolean doc) throws SQLException {
        return update("UPDATE Expenses SET has_document = ? WHERE expense_id = ?", doc, id) == 1;
    }

    public boolean deleteExpense(int id) throws SQLException {
        return update("DELETE FROM Expenses WHERE expense_id = ?", id) == 1;
    }

    public List<Map<String, Object>> expenseByMonth(int months) throws SQLException {
        return query("SELECT FORMAT(spent_on, 'yyyy-MM') AS month, SUM(amount) AS total FROM Expenses WHERE spent_on >= DATEADD(MONTH, ?, GETDATE()) GROUP BY FORMAT(spent_on, 'yyyy-MM') ORDER BY month", -months);
    }

    public List<Map<String, Object>> reconciliation(int days) throws SQLException {
        String sql = "SELECT o.order_id, o.order_code, o.amount AS order_amount, o.created_at, i.invoice_id, i.invoice_no, i.total_amount AS invoice_amount, i.status AS invoice_status, "
                + "(SELECT COUNT(*) FROM Orders d WHERE d.order_id <> o.order_id AND d.created_by = o.created_by AND d.amount = o.amount AND d.status = N'COMPLETED' "
                + "AND ABS(DATEDIFF(SECOND, d.created_at, o.created_at)) <= 60) AS twins "
                + "FROM Orders o LEFT JOIN Invoices i ON i.source_type = N'ORDER' AND i.source_id = o.order_id "
                + "WHERE o.status = N'COMPLETED' AND o.created_at >= DATEADD(DAY, ?, GETDATE()) ORDER BY o.created_at DESC";
        return query(sql, -days);
    }

    public List<Map<String, Object>> orphanInvoices() throws SQLException {
        return query("SELECT i.invoice_id, i.invoice_no, i.total_amount, i.status, i.source_type, i.source_id FROM Invoices i "
                + "WHERE (i.source_type = N'ORDER' AND NOT EXISTS (SELECT 1 FROM Orders o WHERE o.order_id = i.source_id AND o.status = N'COMPLETED')) "
                + "OR (i.source_type = N'CHANNEL_SALE' AND NOT EXISTS (SELECT 1 FROM ChannelSales c WHERE c.sale_id = i.source_id AND c.status = N'COMPLETED'))");
    }

    public Map<String, Map<String, Object>> reconNotes() throws SQLException {
        Map<String, Map<String, Object>> map = new LinkedHashMap<>();
        for (Map<String, Object> r : query("SELECT ref_key, note, resolved FROM TaxReconNotes")) {
            map.put((String) r.get("ref_key"), r);
        }
        return map;
    }

    public void saveReconNote(String key, String note, boolean resolved, int userId) throws SQLException {
        int n = update("UPDATE TaxReconNotes SET note = ?, resolved = ?, updated_by = ?, updated_at = SYSDATETIME() WHERE ref_key = ?", note, resolved, userId, key);
        if (n == 0) {
            update("INSERT INTO TaxReconNotes (ref_key, note, resolved, updated_by) VALUES (?, ?, ?, ?)", key, note, resolved, userId);
        }
    }

    public int scalar(String sql, Object... args) throws SQLException {
        List<Map<String, Object>> rows = query(sql, args);
        if (rows.isEmpty()) {
            return 0;
        }
        Object v = rows.get(0).values().iterator().next();
        return v == null ? 0 : ((Number) v).intValue();
    }
}
