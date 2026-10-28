import dao.ChannelAnalyticsDAO;
import java.sql.SQLException;
import java.util.List;
import service.ChannelAnalyticsService;

public class AnalyticsTest {

    private static int failures = 0;

    private static void check(String name, boolean condition) {
        if (!condition) {
            failures++;
        }
        System.out.println((condition ? "OK   " : "FAIL ") + name);
    }

    public static void main(String[] args) {
        ChannelAnalyticsService svc = new ChannelAnalyticsService();
        try {
            ChannelAnalyticsService.AnalyticsBundle b = svc.getAnalytics("30d", "ALL");
            check("analytics bundle returned", b != null);
            check("channel metrics count is 3 (STORE, SHOPEE, TIKTOK)", b.channelMetrics.size() == 3);
            check("store metric exists", b.channelMetrics.stream().anyMatch(m -> "STORE".equals(m.channel)));
            check("shopee metric exists", b.channelMetrics.stream().anyMatch(m -> "SHOPEE".equals(m.channel)));
            check("tiktok metric exists", b.channelMetrics.stream().anyMatch(m -> "TIKTOK".equals(m.channel)));
            check("shopee waterfall exists", b.shopeeWaterfall != null && "SHOPEE".equals(b.shopeeWaterfall.channel));
            check("tiktok waterfall exists", b.tiktokWaterfall != null && "TIKTOK".equals(b.tiktokWaterfall.channel));
            check("availability object exists", b.availability != null);
            check("top skus list not null", b.topSkus != null);
            check("traffic list not null", b.trafficMetrics != null);
            check("daily trends not null", b.dailyTrends != null);
        } catch (SQLException e) {
            failures++;
            System.out.println("FAIL database error: " + e.getMessage());
        }
        System.out.println(failures == 0 ? "ALL OK" : failures + " FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }
}

