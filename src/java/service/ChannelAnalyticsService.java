package service;

import dao.ChannelAnalyticsDAO;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public class ChannelAnalyticsService {

    private final ChannelAnalyticsDAO dao = new ChannelAnalyticsDAO();

    public static class AnalyticsBundle {
        public String period;
        public String channel;
        public List<ChannelAnalyticsDAO.ChannelMetric> channelMetrics;
        public ChannelAnalyticsDAO.FeeWaterfall shopeeWaterfall;
        public ChannelAnalyticsDAO.FeeWaterfall tiktokWaterfall;
        public List<ChannelAnalyticsDAO.SkuPerformance> topSkus;
        public List<ChannelAnalyticsDAO.TrafficMetric> trafficMetrics;
        public List<ChannelAnalyticsDAO.DailyTrend> dailyTrends;
        public ChannelAnalyticsDAO.DataAvailability availability;

        public String getPeriod() { return period; }
        public String getChannel() { return channel; }
        public List<ChannelAnalyticsDAO.ChannelMetric> getChannelMetrics() { return channelMetrics; }
        public ChannelAnalyticsDAO.FeeWaterfall getShopeeWaterfall() { return shopeeWaterfall; }
        public ChannelAnalyticsDAO.FeeWaterfall getTiktokWaterfall() { return tiktokWaterfall; }
        public List<ChannelAnalyticsDAO.SkuPerformance> getTopSkus() { return topSkus; }
        public List<ChannelAnalyticsDAO.TrafficMetric> getTrafficMetrics() { return trafficMetrics; }
        public List<ChannelAnalyticsDAO.DailyTrend> getDailyTrends() { return dailyTrends; }
        public ChannelAnalyticsDAO.DataAvailability getAvailability() { return availability; }
    }

    public AnalyticsBundle getAnalytics(String period, String channel) throws SQLException {
        Timestamp[] range = resolveRange(period);
        Timestamp from = range[0];
        Timestamp to = range[1];

        AnalyticsBundle b = new AnalyticsBundle();
        b.period = period != null ? period : "30d";
        b.channel = channel != null ? channel : "ALL";

        b.availability = dao.checkAvailability(from, to);
        b.channelMetrics = dao.getChannelComparison(from, to);
        b.shopeeWaterfall = dao.getFeeWaterfall("SHOPEE", from, to);
        b.tiktokWaterfall = dao.getFeeWaterfall("TIKTOK", from, to);
        b.topSkus = dao.getTopSkus("ALL".equals(b.channel) ? null : b.channel, from, to, 10);
        b.trafficMetrics = dao.getTrafficAttribution("ALL".equals(b.channel) ? null : b.channel, from, to);
        b.dailyTrends = dao.getDailyTrends(from, to, 14);

        return b;
    }

    private Timestamp[] resolveRange(String period) {
        if (period == null || period.isEmpty()) {
            period = "30d";
        }
        LocalDate today = LocalDate.now();
        LocalDateTime from;
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        switch (period.toLowerCase()) {
            case "today":
                from = today.atStartOfDay();
                break;
            case "7d":
                from = today.minusDays(7).atStartOfDay();
                break;
            case "month":
                from = YearMonth.now().atDay(1).atStartOfDay();
                break;
            case "all":
                from = null;
                to = null;
                break;
            case "30d":
            default:
                from = today.minusDays(30).atStartOfDay();
                break;
        }

        Timestamp f = from != null ? Timestamp.valueOf(from) : null;
        Timestamp t = to != null ? Timestamp.valueOf(to) : null;
        return new Timestamp[]{f, t};
    }
}

