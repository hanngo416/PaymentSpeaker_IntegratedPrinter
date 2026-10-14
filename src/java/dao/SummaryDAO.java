package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import model.view.Summary;
import util.DBUtil;

/**
 * Money totals of a time window [from, to): paid orders (what should have come in) against every transfer
 * that reached the account (what really came in).
 */
public class SummaryDAO {

    public Summary summarize(Timestamp from, Timestamp to) throws SQLException {
        Summary s = new Summary();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*), ISNULL(SUM(amount), 0), ISNULL(SUM(cash_amount), 0) FROM Orders "
                    + "WHERE status = N'PAID' AND created_at >= ? AND created_at < ?")) {
                window(ps, from, to);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    s.setOrderCount(rs.getInt(1));
                    s.setTotal(rs.getBigDecimal(2));
                    s.setCash(rs.getBigDecimal(3));
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM Orders WHERE status = N'PENDING' AND created_at >= ? AND created_at < ?")) {
                window(ps, from, to);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    s.setPendingOrders(rs.getInt(1));
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*), ISNULL(SUM(amount), 0) FROM Transactions WHERE trans_time >= ? AND trans_time < ?")) {
                window(ps, from, to);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    s.setBankCount(rs.getInt(1));
                    s.setBankReceived(rs.getBigDecimal(2));
                }
            }
        }
        return s;
    }

    // One line per day (labelLength 10, yyyy-mm-dd) or per month (labelLength 7, yyyy-mm), oldest first
    public List<Summary> breakdown(Timestamp from, Timestamp to, int labelLength) throws SQLException {
        Map<String, Summary> map = new TreeMap<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT CONVERT(VARCHAR(" + labelLength + "), created_at, 120) AS k, COUNT(*), SUM(amount), SUM(cash_amount) "
                    + "FROM Orders WHERE status = N'PAID' AND created_at >= ? AND created_at < ? "
                    + "GROUP BY CONVERT(VARCHAR(" + labelLength + "), created_at, 120)")) {
                window(ps, from, to);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Summary s = line(map, rs.getString(1));
                        s.setOrderCount(rs.getInt(2));
                        s.setTotal(rs.getBigDecimal(3));
                        s.setCash(rs.getBigDecimal(4));
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT CONVERT(VARCHAR(" + labelLength + "), trans_time, 120) AS k, COUNT(*), SUM(amount) "
                    + "FROM Transactions WHERE trans_time >= ? AND trans_time < ? "
                    + "GROUP BY CONVERT(VARCHAR(" + labelLength + "), trans_time, 120)")) {
                window(ps, from, to);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Summary s = line(map, rs.getString(1));
                        s.setBankCount(rs.getInt(2));
                        s.setBankReceived(rs.getBigDecimal(3));
                    }
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    private static Summary line(Map<String, Summary> map, String label) {
        Summary s = map.get(label);
        if (s == null) {
            s = new Summary();
            s.setLabel(label);
            map.put(label, s);
        }
        return s;
    }

    private static void window(PreparedStatement ps, Timestamp from, Timestamp to) throws SQLException {
        ps.setTimestamp(1, from);
        ps.setTimestamp(2, to);
    }
}
