/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import util.DBUtil;

/**
 *
 * @author PC
 */
public class TaxDocumentDAO {

    private static final String COLUMNS = "d.doc_id, d.doc_name, d.category, d.period_label, d.status, d.file_name, d.size_bytes, d.uploaded_at, u.full_name AS uploaded_name";

    private final TaxToolsDAO db = new TaxToolsDAO();

    public List<Map<String, Object>> search(String q, String category, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM TaxDocuments d LEFT JOIN Users u ON u.user_id = d.uploaded_by WHERE 1 = 1 ");
        java.util.List<Object> args = new java.util.ArrayList<>();
        if (!q.isEmpty()) {
            sql.append("AND (d.doc_name LIKE ? OR d.file_name LIKE ? OR d.period_label LIKE ?) ");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
            args.add("%" + q + "%");
        }
        if (!category.isEmpty()) {
            sql.append("AND d.category = ? ");
            args.add(category);
        }
        if (!status.isEmpty()) {
            sql.append("AND d.status = ? ");
            args.add(status);
        }
        sql.append("ORDER BY CASE d.status WHEN 'MISSING' THEN 0 WHEN 'REVIEW' THEN 1 ELSE 2 END, d.doc_id DESC");
        return db.query(sql.toString(), args.toArray());
    }

    public Map<String, Object> counts() throws SQLException {
        return db.query("SELECT COUNT(*) AS total, "
                + "SUM(CASE WHEN status = 'UPLOADED' THEN 1 ELSE 0 END) AS uploaded, "
                + "SUM(CASE WHEN status = 'MISSING' THEN 1 ELSE 0 END) AS missing, "
                + "SUM(CASE WHEN status = 'REVIEW' THEN 1 ELSE 0 END) AS review FROM TaxDocuments").get(0);
    }

    public Map<String, Object> find(int id) throws SQLException {
        List<Map<String, Object>> rows = db.query("SELECT " + COLUMNS + " FROM TaxDocuments d LEFT JOIN Users u ON u.user_id = d.uploaded_by WHERE d.doc_id = ?", id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int insert(String name, String category, String period, String fileName, String contentType, int size, InputStream in, int userId) throws SQLException, IOException {
        String sql = "INSERT INTO TaxDocuments (doc_name, category, period_label, status, file_name, content_type, size_bytes, content, uploaded_by, uploaded_at) "
                + "VALUES (?, ?, ?, 'UPLOADED', ?, ?, ?, ?, ?, SYSDATETIME())";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, period.isEmpty() ? null : period);
            ps.setString(4, fileName);
            ps.setString(5, contentType);
            ps.setInt(6, size);
            ps.setBinaryStream(7, in, size);
            ps.setInt(8, userId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        }
    }

    public boolean attach(int id, String fileName, String contentType, int size, InputStream in, int userId) throws SQLException {
        String sql = "UPDATE TaxDocuments SET status = 'UPLOADED', file_name = ?, content_type = ?, size_bytes = ?, content = ?, uploaded_by = ?, uploaded_at = SYSDATETIME() "
                + "WHERE doc_id = ? AND status = 'MISSING'";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fileName);
            ps.setString(2, contentType);
            ps.setInt(3, size);
            ps.setBinaryStream(4, in, size);
            ps.setInt(5, userId);
            ps.setInt(6, id);
            return ps.executeUpdate() == 1;
        }
    }

    public boolean setReview(int id, boolean review) throws SQLException {
        String sql = "UPDATE TaxDocuments SET status = ? WHERE doc_id = ? AND status = ?";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, review ? "REVIEW" : "UPLOADED");
            ps.setInt(2, id);
            ps.setString(3, review ? "UPLOADED" : "REVIEW");
            return ps.executeUpdate() == 1;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM TaxDocuments WHERE doc_id = ? AND status <> 'MISSING'")) {
            ps.setInt(1, id);
            return ps.executeUpdate() == 1;
        }
    }

    public Object[] file(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT file_name, content_type, content FROM TaxDocuments WHERE doc_id = ? AND content IS NOT NULL")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Object[]{rs.getString(1), rs.getString(2), rs.getBytes(3)} : null;
            }
        }
    }
}
