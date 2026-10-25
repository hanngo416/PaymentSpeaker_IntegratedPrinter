import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KnowledgeSyncTest {

    static int failed = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "OK   " : "FAIL ") + name);
        if (!ok) {
            failed++;
        }
    }

    static String read(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    static JsonObject find(JsonArray list, String id) {
        for (JsonElement e : list) {
            if (id.equals(e.getAsJsonObject().get("id").getAsString())) {
                return e.getAsJsonObject();
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        String businessRaw = read("src/java/business_docs.json");
        String taxRaw = read("src/java/taxdocs.json");
        String seed = read("sql/02_seed.sql");
        JsonObject business = JsonParser.parseString(businessRaw).getAsJsonObject();
        JsonObject tax = JsonParser.parseString(taxRaw).getAsJsonObject();
        JsonArray articles = business.getAsJsonArray("articles");
        JsonArray docs = tax.getAsJsonArray("docs");

        check("both files are not empty", articles.size() > 0 && docs.size() > 0);

        JsonObject threshold = find(articles, "tax-threshold-1bn");
        String thresholdText = threshold.getAsJsonObject("content").toString();
        check("seed THRESHOLD is 1,000,000,000", seed.contains("N'THRESHOLD', NULL, 1000000000"));
        check("tax-threshold article uses the 1 billion threshold", thresholdText.contains("1.000.000.000") && thresholdText.contains("1 tỷ"));
        check("tax-threshold article says households under the threshold still notify revenue", thresholdText.contains("thông báo doanh thu"));
        check("taxdocs threshold-1bn says the same", find(docs, "threshold-1bn").get("text").getAsString().contains("vẫn phải thông báo doanh thu"));

        Set<String> allowed = new HashSet<>(Arrays.asList("GOODS", "SERVICE", "PRODUCTION", "OTHER"));
        Matcher m = Pattern.compile("\b[A-Z]{4,}(?:_[A-Z]+)*\b").matcher(threshold.getAsJsonObject("content").get("steps").toString());
        boolean sectorsOk = true;
        while (m.find()) {
            if (!allowed.contains(m.group())) {
                sectorsOk = false;
                System.out.println("     unknown sector code: " + m.group());
            }
        }
        check("tax-threshold steps use only the four Tinky sectors", sectorsOk);
        check("no FOOD_BEVERAGE sector anywhere", !businessRaw.contains("FOOD_BEVERAGE") && !taxRaw.contains("FOOD_BEVERAGE"));

        boolean staleThreshold = false;
        for (JsonElement e : articles) {
            String content = e.getAsJsonObject().getAsJsonObject("content").toString();
            if (Pattern.compile("ngưỡng[^.\"]{0,40}500 triệu").matcher(content).find()) {
                staleThreshold = true;
            }
        }
        check("no article states 500 million as the threshold", !staleThreshold);

        String[][] rates = {{"GOODS", "1", "0.5"}, {"SERVICE", "5", "2"}, {"PRODUCTION", "3", "1.5"}, {"OTHER", "2", "1"}};
        boolean ratesOk = true;
        for (String[] r : rates) {
            ratesOk &= Pattern.compile("N'VAT_RATE', N'" + r[0] + "', " + r[1] + ",").matcher(seed).find();
            ratesOk &= Pattern.compile("N'PIT_RATE', N'" + r[0] + "', " + r[2] + ",").matcher(seed).find();
        }
        check("seed rates equal the rates written in the guide article", ratesOk);

        String bank = find(articles, "bank-account-01bkstk").getAsJsonObject("content").get("when_to_do").getAsString();
        check("bank account article has no invented 10-day deadline", !bank.contains("10 ngày"));

        String permits = find(articles, "permits-attp-pccc").getAsJsonObject("content").get("overview").getAsString();
        check("permits article mentions the exemption (Dieu 12 Nghi dinh 15/2018)", permits.contains("miễn cấp giấy"));

        String[] needNote = {"tax-threshold-1bn", "bank-account-01bkstk", "permits-attp-pccc", "register-hkd-2026", "bookkeeping-tt152", "legal-models"};
        boolean notes = true;
        for (String id : needNote) {
            JsonObject a = find(articles, id);
            notes &= a != null && a.has("verify_note") && !a.get("verify_note").getAsString().isEmpty();
        }
        check("articles that overlap the legal guide carry a verify note", notes);

        System.out.println(failed == 0 ? "ALL OK" : failed + " FAILED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
