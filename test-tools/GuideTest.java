import dao.GuideDAO;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import model.GuideArticle;
import model.GuideCategory;
import service.BusinessDocService;

public class GuideTest {

    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "OK   " : "FAIL ") + name);
        if (!ok) {
            failed++;
        }
    }

    public static void main(String[] args) {
        BusinessDocService service = new BusinessDocService();
        check("service is available", service.isAvailable());

        List<GuideCategory> categories = service.getCategories();
        check("exactly 6 categories", categories.size() == 6);

        List<GuideArticle> articles = service.getArticles();
        check("all 26 articles loaded", articles.size() == 26);

        boolean allValid = true;
        for (GuideArticle a : articles) {
            if (a.getId() == null || a.getId().isEmpty()
                    || a.getTitle() == null || a.getTitle().isEmpty()
                    || a.getCategoryId() == null || a.getCategoryId().isEmpty()
                    || a.getSummary() == null || a.getSummary().isEmpty()
                    || a.getOverview() == null || a.getOverview().isEmpty()
                    || a.getSteps() == null || a.getSteps().isEmpty()
                    || a.getChecklist() == null || a.getChecklist().isEmpty()
                    || a.getSources() == null || a.getSources().isEmpty()) {
                allValid = false;
                break;
            }
        }
        check("all articles have mandatory fields", allValid);

        check("category preparation exists", service.getCategory("preparation") != null);
        check("category legal-tax exists", service.getCategory("legal-tax") != null);
        check("category pos-operations exists", service.getCategory("pos-operations") != null);
        check("category shopee-seller exists", service.getCategory("shopee-seller") != null);
        check("category tiktok-seller exists", service.getCategory("tiktok-seller") != null);
        check("category finance-omnichannel exists", service.getCategory("finance-omnichannel") != null);

        List<GuideArticle> shopeeArticles = service.getArticlesByCategory("shopee-seller");
        check("shopee category has 4 articles", shopeeArticles.size() == 4);

        List<GuideArticle> tiktokArticles = service.getArticlesByCategory("tiktok-seller");
        check("tiktok category has 4 articles", tiktokArticles.size() == 4);

        List<GuideArticle> searchShopee = service.search("shopee", 5);
        check("search 'shopee' returns results", !searchShopee.isEmpty());

        List<GuideArticle> searchThue = service.search("thuế khoán hộ kinh doanh", 5);
        check("search 'thuế khoán' returns results", !searchThue.isEmpty());

        List<GuideArticle> searchAccentFolded = service.search("thue khoan ho kinh doanh", 5);
        check("accent-free search matches", !searchAccentFolded.isEmpty());

        GuideDAO dao = new GuideDAO();
        try {
            int testUserId = 1;
            dao.save(testUserId, "prep-product-market", GuideDAO.BOOKMARK, "1");
            dao.save(testUserId, "prep-product-market", GuideDAO.READ, "1");
            dao.save(testUserId, "prep-product-market", GuideDAO.CHECKLIST, "{\"step1\":true}");

            Map<String, String> progress = dao.load(testUserId);
            check("dao loads saved bookmark", "1".equals(progress.get(GuideDAO.key(GuideDAO.BOOKMARK, "prep-product-market"))));
            check("dao loads saved read status", "1".equals(progress.get(GuideDAO.key(GuideDAO.READ, "prep-product-market"))));
            check("dao loads saved checklist", progress.get(GuideDAO.key(GuideDAO.CHECKLIST, "prep-product-market")).contains("step1"));

            dao.save(testUserId, "prep-product-market", GuideDAO.BOOKMARK, "0");
            progress = dao.load(testUserId);
            check("dao updates state cleanly", "0".equals(progress.get(GuideDAO.key(GuideDAO.BOOKMARK, "prep-product-market"))));
        } catch (SQLException e) {
            check("dao operations threw SQLException: " + e.getMessage(), false);
        }

        System.out.println(failed == 0 ? "ALL GUIDE TESTS PASSED" : failed + " TESTS FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
