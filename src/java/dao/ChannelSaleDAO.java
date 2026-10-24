package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.ChannelSale;
import model.Page;
import util.DBUtil;

public class ChannelSaleDAO {

    public static class ChannelSummary {

        private String channel;
        private int orderCount;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal totalFee = BigDecimal.ZERO;
        private BigDecimal netRevenue = BigDecimal.ZERO;

        public String getChannel() {
            return channel;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }

        public BigDecimal getTotalFee() {
            return totalFee;
        }

        public BigDecimal getNetRevenue() {
            return netRevenue;
        }
    }

    private static final String SELECT
            = "SELECT s.sale_id, s.channel, s.external_ref, s.sold_at, s.status, s.goods_amount, s.platform_fee, "
            + "s.shipping_fee, s.affiliate_id, s.affiliate_commission, s.cost_amount, s.note, s.created_by, "
            + "s.created_at, a.name AS affiliate_name "
            + "FROM ChannelSales s LEFT JOIN Affiliates a ON a.affiliate_id = s.affiliate_id ";

    public void insert(ChannelSale s) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            insert(con, s);
        }
    }

    public void insert(Connection con, ChannelSale s) throws SQLException {
        String sql = "INSERT INTO ChannelSales (channel, external_ref, sold_at, status, goods_amount, platform_fee, "
                + "shipping_fee, affiliate_id, affiliate_commission, cost_amount, note, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getChannel());
            ps.setString(2, s.getExternalRef());
            ps.setTimestamp(3, s.getSoldAt());
            ps.setString(4, s.getStatus());
            ps.setBigDecimal(5, s.getGoodsAmount());
            ps.setBigDecimal(6, s.getPlatformFee());
            ps.setBigDecimal(7, s.getShippingFee());
            if (s.getAffiliateId() == null) {
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, s.getAffiliateId());
            }
            ps.setBigDecimal(9, s.getAffiliateCommission());
            ps.setBigDecimal(10, s.getCostAmount());
            ps.setString(11, s.getNote());
            ps.setInt(12, s.getCreatedBy());
            ps.executeUpdate();
        }
    }

    public boolean existsByRef(String channel, String ref) throws SQLException {
        try (Connection con = DBUtil.getConnection()) {
            return existsByRef(con, channel, ref);
        }
    }

    public boolean existsByRef(Connection con, String channel, String ref) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                     "SELECT 1 FROM ChannelSales WHERE channel = ? AND external_ref = ?")) {
            ps.setString(1, channel);
            ps.setString(2, ref);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Page<ChannelSale> search(Timestamp from, Timestamp to, String channelOrNull, int page, int size)
            throws SQLException {
        String where = "WHERE (? IS NULL OR s.sold_at >= ?) AND (? IS NULL OR s.sold_at < ?) "
                + "AND (? IS NULL OR s.channel = ?) ";
        int total;
        List<ChannelSale> list = new ArrayList<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ChannelSales s " + where)) {
                bind(ps, from, to, channelOrNull);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            String sql = SELECT + where + "ORDER BY s.sold_at DESC, s.sale_id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, from, to, channelOrNull);
                ps.setInt(7, (page - 1) * size);
                ps.setInt(8, size);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
        }
        return new Page<>(list, total, page, size);
    }

    public List<ChannelSummary> todaySummaries() throws SQLException {
        List<ChannelSummary> list = new ArrayList<>();
        String sql = "SELECT channel, COUNT(*) AS n, ISNULL(SUM(goods_amount), 0) AS rev, "
                + "ISNULL(SUM(platform_fee + shipping_fee + affiliate_commission), 0) AS fees "
                + "FROM ChannelSales "
                + "WHERE status = N'COMPLETED' AND CAST(sold_at AS DATE) = CAST(GETDATE() AS DATE) "
                + "GROUP BY channel";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ChannelSummary c = new ChannelSummary();
                c.channel = rs.getString("channel");
                c.orderCount = rs.getInt("n");
                c.totalRevenue = rs.getBigDecimal("rev");
                c.totalFee = rs.getBigDecimal("fees");
                c.netRevenue = c.totalRevenue.subtract(c.totalFee);
                list.add(c);
            }
        }
        return list;
    }

    public ChannelSale find(int id) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(SELECT + "WHERE s.sale_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public boolean setStatus(int id, String status) throws SQLException {
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE ChannelSales SET status = ? WHERE sale_id = ?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        }
    }

    private static void bind(PreparedStatement ps, Timestamp from, Timestamp to, String channel) throws SQLException {
        ps.setTimestamp(1, from);
        ps.setTimestamp(2, from);
        ps.setTimestamp(3, to);
        ps.setTimestamp(4, to);
        ps.setString(5, channel);
        ps.setString(6, channel);
    }

    private ChannelSale map(ResultSet rs) throws SQLException {
        ChannelSale s = new ChannelSale();
        s.setSaleId(rs.getInt("sale_id"));
        s.setChannel(rs.getString("channel"));
        s.setExternalRef(rs.getString("external_ref"));
        s.setSoldAt(rs.getTimestamp("sold_at"));
        s.setStatus(rs.getString("status"));
        s.setGoodsAmount(rs.getBigDecimal("goods_amount"));
        s.setPlatformFee(rs.getBigDecimal("platform_fee"));
        s.setShippingFee(rs.getBigDecimal("shipping_fee"));
        int aff = rs.getInt("affiliate_id");
        s.setAffiliateId(rs.wasNull() ? null : aff);
        s.setAffiliateCommission(rs.getBigDecimal("affiliate_commission"));
        s.setCostAmount(rs.getBigDecimal("cost_amount"));
        s.setNote(rs.getString("note"));
        s.setCreatedBy(rs.getInt("created_by"));
        s.setCreatedAt(rs.getTimestamp("created_at"));
        s.setAffiliateName(rs.getString("affiliate_name"));
        return s;
    }
}
