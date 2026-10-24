package service;

import dao.SettingDAO;
import dao.SummaryDAO;
import dao.TaxRuleDAO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import model.TaxParams;
import model.view.ChannelProfit;
import model.view.DayProfit;
import util.DBUtil;

public class ProfitService {

    public static final String STORE = "STORE";

    private static final String STORE_SQL
            = "SELECT CAST(o.created_at AS DATE) AS d, COUNT(*) AS n, "
            + "SUM(" + SummaryDAO.STORE_REVENUE_EXPR + ") AS revenue, "
            + "SUM(c.line_cost) AS cost "
            + "FROM Orders o OUTER APPLY (SELECT ISNULL(SUM(i.quantity * ISNULL(p.cost_price, 0)), 0) AS line_cost "
            + "FROM OrderItems i LEFT JOIN Products p ON p.product_id = i.product_id "
            + "WHERE i.order_id = o.order_id) c WHERE o.status = N'COMPLETED' AND o.created_at >= ? AND o.created_at < ? "
            + "GROUP BY CAST(o.created_at AS DATE)";

    private static final String CHANNEL_SQL
            = "SELECT CAST(sold_at AS DATE) AS d, channel, COUNT(*) AS n, SUM(goods_amount) AS revenue, "
            + "SUM(platform_fee) AS fee, SUM(shipping_fee) AS ship, SUM(affiliate_commission) AS commission, "
            + "SUM(cost_amount) AS cost "
            + "FROM ChannelSales WHERE status = N'COMPLETED' AND sold_at >= ? AND sold_at < ? "
            + "AND (? IS NULL OR channel = ?) GROUP BY CAST(sold_at AS DATE), channel";

    public static BigDecimal profitOf(BigDecimal goods, BigDecimal fee, BigDecimal ship, BigDecimal commission,
            BigDecimal cost, BigDecimal taxReserve) {
        return goods.subtract(fee).subtract(ship).subtract(commission).subtract(cost).subtract(taxReserve);
    }

    public static BigDecimal taxReserve(BigDecimal revenue, TaxParams p) {
        if (p == null || p.getVatRatePct() == null || revenue.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal reduction = p.getVatReductionPct() == null ? BigDecimal.ZERO : p.getVatReductionPct();
        BigDecimal keep = BigDecimal.ONE.subtract(reduction.divide(BigDecimal.valueOf(100)));
        return revenue.multiply(p.getVatRatePct()).divide(BigDecimal.valueOf(100)).multiply(keep)
                .setScale(0, RoundingMode.HALF_UP);
    }

    private final SettingDAO settingDAO = new SettingDAO();
    private final TaxRuleDAO taxRuleDAO = new TaxRuleDAO();

    private TaxParams params() throws SQLException {
        String sector = settingDAO.get("tax_sector", "GOODS");
        return taxRuleDAO.loadParams(sector, Date.valueOf(LocalDate.now()));
    }

    public List<DayProfit> byDay(Timestamp from, Timestamp to, String channelOrNull) throws SQLException {
        TaxParams p = params();
        Map<LocalDate, DayProfit> days = new TreeMap<>();
        try (Connection con = DBUtil.getConnection()) {
            if (channelOrNull == null || STORE.equals(channelOrNull)) {
                try (PreparedStatement ps = con.prepareStatement(STORE_SQL)) {
                    ps.setTimestamp(1, from);
                    ps.setTimestamp(2, to);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            DayProfit d = day(days, rs.getDate(1).toLocalDate());
                            d.setRevenue(d.getRevenue().add(rs.getBigDecimal("revenue")));
                            d.setCost(d.getCost().add(rs.getBigDecimal("cost")));
                        }
                    }
                }
            }
            if (!STORE.equals(channelOrNull)) {
                try (PreparedStatement ps = con.prepareStatement(CHANNEL_SQL)) {
                    ps.setTimestamp(1, from);
                    ps.setTimestamp(2, to);
                    ps.setString(3, channelOrNull);
                    ps.setString(4, channelOrNull);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            DayProfit d = day(days, rs.getDate(1).toLocalDate());
                            d.setRevenue(d.getRevenue().add(rs.getBigDecimal("revenue")));
                            d.setPlatformFee(d.getPlatformFee().add(rs.getBigDecimal("fee")));
                            d.setShippingFee(d.getShippingFee().add(rs.getBigDecimal("ship")));
                            d.setCommission(d.getCommission().add(rs.getBigDecimal("commission")));
                            d.setCost(d.getCost().add(rs.getBigDecimal("cost")));
                        }
                    }
                }
            }
        }
        for (DayProfit d : days.values()) {
            d.setTaxReserve(taxReserve(d.getRevenue(), p));
            d.setProfit(profitOf(d.getRevenue(), d.getPlatformFee(), d.getShippingFee(), d.getCommission(),
                    d.getCost(), d.getTaxReserve()));
        }
        return new ArrayList<>(days.values());
    }

    public List<ChannelProfit> byChannel(Timestamp from, Timestamp to) throws SQLException {
        TaxParams p = params();
        Map<String, ChannelProfit> map = new TreeMap<>();
        try (Connection con = DBUtil.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(STORE_SQL)) {
                ps.setTimestamp(1, from);
                ps.setTimestamp(2, to);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal revenue = rs.getBigDecimal("revenue");
                        BigDecimal profit = profitOf(revenue, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                                rs.getBigDecimal("cost"), taxReserve(revenue, p));
                        add(map, STORE, rs.getInt("n"), revenue, profit);
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(CHANNEL_SQL)) {
                ps.setTimestamp(1, from);
                ps.setTimestamp(2, to);
                ps.setString(3, null);
                ps.setString(4, null);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        BigDecimal revenue = rs.getBigDecimal("revenue");
                        BigDecimal profit = profitOf(revenue, rs.getBigDecimal("fee"), rs.getBigDecimal("ship"),
                                rs.getBigDecimal("commission"), rs.getBigDecimal("cost"), taxReserve(revenue, p));
                        add(map, rs.getString("channel"), rs.getInt("n"), revenue, profit);
                    }
                }
            }
        }
        return new ArrayList<>(map.values());
    }

    private static DayProfit day(Map<LocalDate, DayProfit> days, LocalDate date) {
        DayProfit d = days.get(date);
        if (d == null) {
            d = new DayProfit();
            d.setDay(date);
            days.put(date, d);
        }
        return d;
    }

    private static void add(Map<String, ChannelProfit> map, String channel, int orders, BigDecimal revenue,
            BigDecimal profit) {
        ChannelProfit c = map.get(channel);
        if (c == null) {
            c = new ChannelProfit();
            c.setChannel(channel);
            map.put(channel, c);
        }
        c.setOrders(c.getOrders() + orders);
        c.setRevenue(c.getRevenue().add(revenue));
        c.setProfit(c.getProfit().add(profit));
    }
}
