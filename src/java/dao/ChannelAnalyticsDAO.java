package dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import util.DBUtil;

public class ChannelAnalyticsDAO {

    public static class ChannelMetric {
        public String channel;
        public int completedOrders;
        public int cancelledOrders;
        public int returnedOrders;
        public int totalOrders;
        public BigDecimal gmv = BigDecimal.ZERO;
        public BigDecimal grossRevenue = BigDecimal.ZERO;
        public BigDecimal totalDeductions = BigDecimal.ZERO;
        public BigDecimal netRevenue = BigDecimal.ZERO;
        public BigDecimal cogs = BigDecimal.ZERO;
        public BigDecimal netProfit = BigDecimal.ZERO;
        public BigDecimal profitMarginPct = BigDecimal.ZERO;
        public BigDecimal averageOrderValue = BigDecimal.ZERO;
        public BigDecimal cancelRatePct = BigDecimal.ZERO;
        public BigDecimal returnRatePct = BigDecimal.ZERO;
    }

    public static class FeeWaterfall {
        public String channel;
        public BigDecimal grossRevenue = BigDecimal.ZERO;
        public BigDecimal fixedFee = BigDecimal.ZERO;
        public BigDecimal serviceFee = BigDecimal.ZERO;
        public BigDecimal transactionFee = BigDecimal.ZERO;
        public BigDecimal shippingFee = BigDecimal.ZERO;
        public BigDecimal affiliateFee = BigDecimal.ZERO;
        public BigDecimal totalFees = BigDecimal.ZERO;
        public BigDecimal netRevenue = BigDecimal.ZERO;
        public BigDecimal cogs = BigDecimal.ZERO;
        public BigDecimal taxReserve = BigDecimal.ZERO;
        public BigDecimal netProfit = BigDecimal.ZERO;
        public BigDecimal settledAmount = BigDecimal.ZERO;
        public BigDecimal pendingEscrow = BigDecimal.ZERO;
    }

    public static class SkuPerformance {
        public String skuCode;
        public String productName;
        public String variationName;
        public int unitsSold;
        public BigDecimal revenue = BigDecimal.ZERO;
        public BigDecimal cost = BigDecimal.ZERO;
        public BigDecimal profit = BigDecimal.ZERO;
        public BigDecimal marginPct = BigDecimal.ZERO;
    }

    public static class TrafficMetric {
        public String source;
        public int orderCount;
        public BigDecimal revenue = BigDecimal.ZERO;
        public BigDecimal sharePct = BigDecimal.ZERO;
    }

    public static class DailyTrend {
        public String date;
        public BigDecimal storeRevenue = BigDecimal.ZERO;
        public BigDecimal shopeeRevenue = BigDecimal.ZERO;
        public BigDecimal tiktokRevenue = BigDecimal.ZERO;
        public BigDecimal totalProfit = BigDecimal.ZERO;
    }

    public static class DataAvailability {
        public boolean hasItemData;
        public boolean hasTrafficData;
        public boolean hasSettlementData;
    }

    public DataAvailability checkAvailability(Timestamp from, Timestamp to) throws SQLException {
        DataAvailability a = new DataAvailability();
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM ChannelSaleItems) AS item_count, "
                + "(SELECT COUNT(*) FROM ChannelSales WHERE traffic_source IS NOT NULL) AS traffic_count, "
                + "(SELECT COUNT(*) FROM ChannelSales WHERE settlement_amount IS NOT NULL) AS settle_count";
        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                a.hasItemData = rs.getInt("item_count") > 0;
                a.hasTrafficData = rs.getInt("traffic_count") > 0;
                a.hasSettlementData = rs.getInt("settle_count") > 0;
            }
        }
        return a;
    }

    public List<ChannelMetric> getChannelComparison(Timestamp from, Timestamp to) throws SQLException {
        List<ChannelMetric> list = new ArrayList<>();
        list.add(getStoreMetric(from, to));
        list.add(getPlatformMetric("SHOPEE", from, to));
        list.add(getPlatformMetric("TIKTOK", from, to));
        return list;
    }

    private ChannelMetric getStoreMetric(Timestamp from, Timestamp to) throws SQLException {
        ChannelMetric m = new ChannelMetric();
        m.channel = "STORE";
        String sql = "SELECT "
                + "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed_cnt, "
                + "COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_cnt, "
                + "COUNT(*) AS total_cnt, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END), 0) AS rev, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN discount_amount ELSE 0 END), 0) AS disc "
                + "FROM Orders WHERE (? IS NULL OR created_at >= ?) AND (? IS NULL OR created_at < ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, to);
            ps.setTimestamp(4, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.completedOrders = rs.getInt("completed_cnt");
                    m.cancelledOrders = rs.getInt("cancelled_cnt");
                    m.totalOrders = rs.getInt("total_cnt");
                    m.grossRevenue = rs.getBigDecimal("rev");
                    m.gmv = m.grossRevenue.add(rs.getBigDecimal("disc"));
                    m.netRevenue = m.grossRevenue;
                    m.totalDeductions = BigDecimal.ZERO;
                }
            }
        }
        String cogsSql = "SELECT COALESCE(SUM(i.quantity * p.cost_price), 0) AS cogs "
                + "FROM OrderItems i "
                + "JOIN Orders o ON o.order_id = i.order_id "
                + "LEFT JOIN Products p ON p.product_id = i.product_id "
                + "WHERE o.status = 'COMPLETED' AND (? IS NULL OR o.created_at >= ?) AND (? IS NULL OR o.created_at < ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(cogsSql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, to);
            ps.setTimestamp(4, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.cogs = rs.getBigDecimal("cogs");
                }
            }
        }
        m.netProfit = m.netRevenue.subtract(m.cogs);
        calculateRatios(m);
        return m;
    }

    private ChannelMetric getPlatformMetric(String channel, Timestamp from, Timestamp to) throws SQLException {
        ChannelMetric m = new ChannelMetric();
        m.channel = channel;
        String sql = "SELECT "
                + "COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) AS completed_cnt, "
                + "COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) AS cancelled_cnt, "
                + "COUNT(CASE WHEN status = 'RETURNED' THEN 1 END) AS returned_cnt, "
                + "COUNT(*) AS total_cnt, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN buyer_paid_amount ELSE 0 END), 0) AS gmv_sum, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN goods_amount ELSE 0 END), 0) AS gross_rev, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN platform_fee ELSE 0 END), 0) AS plat_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN shipping_fee ELSE 0 END), 0) AS ship_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN affiliate_commission ELSE 0 END), 0) AS aff_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN cost_amount ELSE 0 END), 0) AS cogs_sum "
                + "FROM ChannelSales WHERE channel = ? AND (? IS NULL OR sold_at >= ?) AND (? IS NULL OR sold_at < ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, to);
            ps.setTimestamp(5, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    m.completedOrders = rs.getInt("completed_cnt");
                    m.cancelledOrders = rs.getInt("cancelled_cnt");
                    m.returnedOrders = rs.getInt("returned_cnt");
                    m.totalOrders = rs.getInt("total_cnt");
                    m.grossRevenue = rs.getBigDecimal("gross_rev");
                    BigDecimal gmvVal = rs.getBigDecimal("gmv_sum");
                    m.gmv = gmvVal.compareTo(BigDecimal.ZERO) > 0 ? gmvVal : m.grossRevenue;
                    BigDecimal pf = rs.getBigDecimal("plat_fee");
                    BigDecimal sf = rs.getBigDecimal("ship_fee");
                    BigDecimal af = rs.getBigDecimal("aff_fee");
                    m.totalDeductions = pf.add(sf).add(af);
                    m.netRevenue = m.grossRevenue.subtract(m.totalDeductions);
                    m.cogs = rs.getBigDecimal("cogs_sum");
                    m.netProfit = m.netRevenue.subtract(m.cogs);
                }
            }
        }
        calculateRatios(m);
        return m;
    }

    private void calculateRatios(ChannelMetric m) {
        if (m.grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            m.profitMarginPct = m.netProfit.multiply(BigDecimal.valueOf(100))
                    .divide(m.grossRevenue, 1, RoundingMode.HALF_UP);
        }
        if (m.completedOrders > 0) {
            m.averageOrderValue = m.grossRevenue.divide(BigDecimal.valueOf(m.completedOrders), 0, RoundingMode.HALF_UP);
        }
        if (m.totalOrders > 0) {
            m.cancelRatePct = BigDecimal.valueOf(m.cancelledOrders * 100.0 / m.totalOrders).setScale(1, RoundingMode.HALF_UP);
            m.returnRatePct = BigDecimal.valueOf(m.returnedOrders * 100.0 / m.totalOrders).setScale(1, RoundingMode.HALF_UP);
        }
    }

    public FeeWaterfall getFeeWaterfall(String channel, Timestamp from, Timestamp to) throws SQLException {
        FeeWaterfall f = new FeeWaterfall();
        f.channel = channel;
        String sql = "SELECT "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN goods_amount ELSE 0 END), 0) AS gross_rev, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN platform_fee ELSE 0 END), 0) AS plat_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN service_fee ELSE 0 END), 0) AS srv_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN transaction_fee ELSE 0 END), 0) AS trx_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN shipping_fee ELSE 0 END), 0) AS ship_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN affiliate_commission ELSE 0 END), 0) AS aff_fee, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN cost_amount ELSE 0 END), 0) AS cogs_sum, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' AND settlement_status = 'SETTLED' THEN COALESCE(settlement_amount, goods_amount - platform_fee - shipping_fee - affiliate_commission) ELSE 0 END), 0) AS settled_sum, "
                + "COALESCE(SUM(CASE WHEN status = 'COMPLETED' AND (settlement_status != 'SETTLED' OR settlement_amount IS NULL) THEN (goods_amount - platform_fee - shipping_fee - affiliate_commission) ELSE 0 END), 0) AS pending_sum "
                + "FROM ChannelSales WHERE channel = ? AND (? IS NULL OR sold_at >= ?) AND (? IS NULL OR sold_at < ?)";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, channel);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, to);
            ps.setTimestamp(5, to);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    f.grossRevenue = rs.getBigDecimal("gross_rev");
                    f.fixedFee = rs.getBigDecimal("plat_fee");
                    f.serviceFee = rs.getBigDecimal("srv_fee");
                    f.transactionFee = rs.getBigDecimal("trx_fee");
                    f.shippingFee = rs.getBigDecimal("ship_fee");
                    f.affiliateFee = rs.getBigDecimal("aff_fee");
                    f.totalFees = f.fixedFee.add(f.serviceFee).add(f.transactionFee).add(f.shippingFee).add(f.affiliateFee);
                    f.netRevenue = f.grossRevenue.subtract(f.totalFees);
                    f.cogs = rs.getBigDecimal("cogs_sum");
                    f.netProfit = f.netRevenue.subtract(f.cogs);
                    f.settledAmount = rs.getBigDecimal("settled_sum");
                    f.pendingEscrow = rs.getBigDecimal("pending_sum");
                }
            }
        }
        return f;
    }

    public List<SkuPerformance> getTopSkus(String channelOrNull, Timestamp from, Timestamp to, int limit) throws SQLException {
        List<SkuPerformance> list = new ArrayList<>();
        String sql = "SELECT i.sku_code, MAX(i.product_name) AS prod_name, MAX(i.variation_name) AS var_name, "
                + "SUM(i.quantity) AS qty, SUM(i.subtotal) AS rev, SUM(i.cost_price * i.quantity) AS cogs "
                + "FROM ChannelSaleItems i "
                + "JOIN ChannelSales s ON s.sale_id = i.sale_id "
                + "WHERE s.status = 'COMPLETED' AND (? IS NULL OR s.channel = ?) "
                + "AND (? IS NULL OR s.sold_at >= ?) AND (? IS NULL OR s.sold_at < ?) "
                + "GROUP BY i.sku_code ORDER BY rev DESC";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, channelOrNull);
            ps.setString(2, channelOrNull);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, from);
            ps.setTimestamp(5, to);
            ps.setTimestamp(6, to);
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next() && count < limit) {
                    SkuPerformance s = new SkuPerformance();
                    s.skuCode = rs.getString("sku_code");
                    s.productName = rs.getString("prod_name");
                    s.variationName = rs.getString("var_name");
                    s.unitsSold = rs.getInt("qty");
                    s.revenue = rs.getBigDecimal("rev");
                    s.cost = rs.getBigDecimal("cogs");
                    s.profit = s.revenue.subtract(s.cost);
                    if (s.revenue.compareTo(BigDecimal.ZERO) > 0) {
                        s.marginPct = s.profit.multiply(BigDecimal.valueOf(100)).divide(s.revenue, 1, RoundingMode.HALF_UP);
                    }
                    list.add(s);
                    count++;
                }
            }
        }
        return list;
    }

    public List<TrafficMetric> getTrafficAttribution(String channelOrNull, Timestamp from, Timestamp to) throws SQLException {
        List<TrafficMetric> list = new ArrayList<>();
        String sql = "SELECT COALESCE(traffic_source, 'Organic / Search') AS src, "
                + "COUNT(*) AS order_cnt, SUM(goods_amount) AS rev "
                + "FROM ChannelSales "
                + "WHERE status = 'COMPLETED' AND (? IS NULL OR channel = ?) "
                + "AND (? IS NULL OR sold_at >= ?) AND (? IS NULL OR sold_at < ?) "
                + "GROUP BY traffic_source ORDER BY rev DESC";
        BigDecimal totalRev = BigDecimal.ZERO;
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, channelOrNull);
            ps.setString(2, channelOrNull);
            ps.setTimestamp(3, from);
            ps.setTimestamp(4, from);
            ps.setTimestamp(5, to);
            ps.setTimestamp(6, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrafficMetric t = new TrafficMetric();
                    t.source = rs.getString("src");
                    t.orderCount = rs.getInt("order_cnt");
                    t.revenue = rs.getBigDecimal("rev");
                    totalRev = totalRev.add(t.revenue);
                    list.add(t);
                }
            }
        }
        if (totalRev.compareTo(BigDecimal.ZERO) > 0) {
            for (TrafficMetric t : list) {
                t.sharePct = t.revenue.multiply(BigDecimal.valueOf(100)).divide(totalRev, 1, RoundingMode.HALF_UP);
            }
        }
        return list;
    }

    public List<DailyTrend> getDailyTrends(Timestamp from, Timestamp to, int limit) throws SQLException {
        List<DailyTrend> list = new ArrayList<>();
        String sql = "SELECT CONVERT(VARCHAR(10), sold_at, 120) AS d, "
                + "COALESCE(SUM(CASE WHEN channel = 'SHOPEE' THEN goods_amount ELSE 0 END), 0) AS shopee_rev, "
                + "COALESCE(SUM(CASE WHEN channel = 'TIKTOK' THEN goods_amount ELSE 0 END), 0) AS tiktok_rev, "
                + "COALESCE(SUM(goods_amount - platform_fee - shipping_fee - affiliate_commission - cost_amount), 0) AS net_profit "
                + "FROM ChannelSales "
                + "WHERE status = 'COMPLETED' AND (? IS NULL OR sold_at >= ?) AND (? IS NULL OR sold_at < ?) "
                + "GROUP BY CONVERT(VARCHAR(10), sold_at, 120) "
                + "ORDER BY d DESC";
        try (Connection con = DBUtil.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, from);
            ps.setTimestamp(2, from);
            ps.setTimestamp(3, to);
            ps.setTimestamp(4, to);
            try (ResultSet rs = ps.executeQuery()) {
                int c = 0;
                while (rs.next() && c < limit) {
                    DailyTrend t = new DailyTrend();
                    t.date = rs.getString("d");
                    t.shopeeRevenue = rs.getBigDecimal("shopee_rev");
                    t.tiktokRevenue = rs.getBigDecimal("tiktok_rev");
                    t.totalProfit = rs.getBigDecimal("net_profit");
                    list.add(t);
                    c++;
                }
            }
        }
        return list;
    }
}

