package dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import util.DBUtil;

public class ChannelReportDAO {

    public static final Map<String, String> TYPES = new LinkedHashMap<>();

    static {
        TYPES.put("sales_by_affiliate", "Sales by affiliate partner");
        TYPES.put("commission_by_affiliate", "Commission by affiliate partner");
        TYPES.put("commission_by_channel", "Commission by platform");
        TYPES.put("outstanding", "Outstanding commission liability");
        TYPES.put("paid_history", "Paid commission history");
        TYPES.put("performance_by_date", "Affiliate performance by date");
        TYPES.put("rejected", "Rejected and cancelled commissions");
        TYPES.put("reconciliation", "Payout reconciliation");
    }

    public static class Table {

        public String[] header = new String[0];
        public List<String[]> rows = new ArrayList<>();
    }

    private static final String VALUE = "ISNULL(c.approved_amount, ISNULL(c.reported_amount, c.calculated_amount))";

    public Table run(String type, Date from, Date to, String channel, Integer affiliateId, String status) throws SQLException {
        List<Object> args = new ArrayList<>();
        String sql;
        switch (type) {
            case "sales_by_affiliate":
                sql = "SELECT a.name AS [Affiliate], COUNT(*) AS [Commission records], SUM(c.base_amount) AS [Sales base (VND)] "
                        + "FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                        + where(args, "c.order_date", from, to, channel, affiliateId, status,
                                status == null || status.isEmpty() ? "AND c.status NOT IN (N'REJECTED', N'CANCELLED') " : "")
                        + "GROUP BY a.name ORDER BY SUM(c.base_amount) DESC";
                break;
            case "commission_by_affiliate":
                sql = "SELECT a.name AS [Affiliate], COUNT(*) AS [Records], SUM(" + VALUE + ") AS [Generated (VND)], "
                        + sums() + " FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                        + where(args, "c.order_date", from, to, channel, affiliateId, null, "")
                        + "GROUP BY a.name ORDER BY SUM(" + VALUE + ") DESC";
                break;
            case "commission_by_channel":
                sql = "SELECT c.channel AS [Platform], COUNT(*) AS [Records], SUM(" + VALUE + ") AS [Generated (VND)], "
                        + sums() + " FROM ChannelCommissions c "
                        + where(args, "c.order_date", from, to, channel, affiliateId, null, "")
                        + "GROUP BY c.channel ORDER BY c.channel";
                break;
            case "outstanding":
                sql = "SELECT a.name AS [Affiliate], COUNT(*) AS [Approved records], SUM(c.approved_amount) AS [Outstanding (VND)], "
                        + "SUM(CASE WHEN c.payout_id IS NULL THEN c.approved_amount ELSE 0 END) AS [Not in a payout (VND)], "
                        + "SUM(CASE WHEN c.payout_id IS NOT NULL THEN c.approved_amount ELSE 0 END) AS [In a draft payout (VND)] "
                        + "FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                        + where(args, "c.order_date", from, to, channel, affiliateId, null, "AND c.status = N'APPROVED' ")
                        + "GROUP BY a.name ORDER BY SUM(c.approved_amount) DESC";
                break;
            case "paid_history":
                sql = "SELECT p.reference AS [Payout], p.payment_date AS [Payment date], p.payment_method AS [Method], "
                        + "p.payment_reference AS [Payment reference], a.name AS [Affiliate], c.channel AS [Platform], "
                        + "c.platform_order_id AS [Platform order], c.approved_amount AS [Amount (VND)] "
                        + "FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                        + "JOIN ChannelPayouts p ON p.payout_id = c.payout_id "
                        + where(args, "p.payment_date", from, to, channel, affiliateId, null, "AND c.status = N'PAID' AND p.status = N'CONFIRMED' ")
                        + "ORDER BY p.payment_date DESC, p.payout_id DESC, c.commission_id";
                break;
            case "performance_by_date":
                sql = "SELECT c.order_date AS [Order date], COUNT(*) AS [Records], SUM(c.base_amount) AS [Sales base (VND)], "
                        + "SUM(" + VALUE + ") AS [Commission (VND)] FROM ChannelCommissions c "
                        + where(args, "c.order_date", from, to, channel, affiliateId, status,
                                status == null || status.isEmpty() ? "AND c.status NOT IN (N'REJECTED', N'CANCELLED') " : "")
                        + "GROUP BY c.order_date ORDER BY c.order_date DESC";
                break;
            case "rejected":
                sql = "SELECT c.order_date AS [Order date], a.name AS [Affiliate], c.channel AS [Platform], c.platform_order_id AS [Platform order], "
                        + "c.status AS [Status], " + "ISNULL(c.reported_amount, c.calculated_amount) AS [Amount (VND)], "
                        + "c.reject_reason AS [Reason], u.full_name AS [Decided by], c.approved_at AS [Decided at] "
                        + "FROM ChannelCommissions c JOIN Affiliates a ON a.affiliate_id = c.affiliate_id "
                        + "LEFT JOIN Users u ON u.user_id = c.approved_by "
                        + where(args, "c.order_date", from, to, channel, affiliateId, null, "AND c.status IN (N'REJECTED', N'CANCELLED') ")
                        + "ORDER BY c.approved_at DESC, c.commission_id DESC";
                break;
            case "reconciliation":
                sql = "SELECT p.reference AS [Payout], a.name AS [Affiliate], p.status AS [Status], p.payment_date AS [Payment date], "
                        + "p.total_amount AS [Payout total (VND)], ISNULL(i.items_sum, 0) AS [Items total (VND)], "
                        + "p.total_amount - ISNULL(i.items_sum, 0) AS [Difference (VND)], ISNULL(i.item_count, 0) AS [Items], "
                        + "ISNULL(k.paid_sum, 0) AS [Commissions marked paid (VND)] "
                        + "FROM ChannelPayouts p JOIN Affiliates a ON a.affiliate_id = p.affiliate_id "
                        + "LEFT JOIN (SELECT payout_id, SUM(amount) AS items_sum, COUNT(*) AS item_count FROM ChannelPayoutItems GROUP BY payout_id) i ON i.payout_id = p.payout_id "
                        + "LEFT JOIN (SELECT payout_id, SUM(approved_amount) AS paid_sum FROM ChannelCommissions WHERE status = N'PAID' GROUP BY payout_id) k ON k.payout_id = p.payout_id "
                        + payoutWhere(args, from, to, affiliateId)
                        + "ORDER BY p.payout_id DESC";
                break;
            default:
                return new Table();
        }
        Table t = new Table();
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < args.size(); i++) {
                ps.setObject(i + 1, args.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int n = md.getColumnCount();
                t.header = new String[n];
                for (int i = 0; i < n; i++) {
                    t.header[i] = md.getColumnLabel(i + 1);
                }
                while (rs.next()) {
                    String[] row = new String[n];
                    for (int i = 0; i < n; i++) {
                        Object v = rs.getObject(i + 1);
                        row[i] = v == null ? "" : (v instanceof java.math.BigDecimal ? ((java.math.BigDecimal) v).toPlainString() : v.toString());
                    }
                    t.rows.add(row);
                }
            }
        }
        return t;
    }

    private static String sums() {
        return "SUM(CASE WHEN c.status = N'PENDING' THEN " + VALUE + " ELSE 0 END) AS [Pending (VND)], "
                + "SUM(CASE WHEN c.status = N'APPROVED' THEN " + VALUE + " ELSE 0 END) AS [Approved unpaid (VND)], "
                + "SUM(CASE WHEN c.status = N'PAID' THEN " + VALUE + " ELSE 0 END) AS [Paid (VND)], "
                + "SUM(CASE WHEN c.status IN (N'REJECTED', N'CANCELLED') THEN " + VALUE + " ELSE 0 END) AS [Rejected or cancelled (VND)] ";
    }

    private static String where(List<Object> args, String dateColumn, Date from, Date to, String channel,
            Integer affiliateId, String status, String extra) {
        StringBuilder sb = new StringBuilder("WHERE 1 = 1 ").append(extra);
        if (from != null) {
            sb.append("AND ").append(dateColumn).append(" >= ? ");
            args.add(from);
        }
        if (to != null) {
            sb.append("AND ").append(dateColumn).append(" <= ? ");
            args.add(to);
        }
        if (channel != null && !channel.isEmpty()) {
            sb.append("AND c.channel = ? ");
            args.add(channel);
        }
        if (affiliateId != null) {
            sb.append("AND c.affiliate_id = ? ");
            args.add(affiliateId);
        }
        if (status != null && !status.isEmpty()) {
            sb.append("AND c.status = ? ");
            args.add(status);
        }
        return sb.toString();
    }

    private static String payoutWhere(List<Object> args, Date from, Date to, Integer affiliateId) {
        StringBuilder sb = new StringBuilder("WHERE 1 = 1 ");
        if (from != null) {
            sb.append("AND CAST(p.created_at AS DATE) >= ? ");
            args.add(from);
        }
        if (to != null) {
            sb.append("AND CAST(p.created_at AS DATE) <= ? ");
            args.add(to);
        }
        if (affiliateId != null) {
            sb.append("AND p.affiliate_id = ? ");
            args.add(affiliateId);
        }
        return sb.toString();
    }
}
