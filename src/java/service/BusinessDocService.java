package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.GuideArticle;
import model.GuideCategory;

public class BusinessDocService {

    public static final String RESOURCE = "/business_docs.json";
    public static final int MAX_QUERY = 100;

    private static List<GuideCategory> categories;
    private static List<GuideArticle> articles;
    private static String updated = "";

    public List<GuideCategory> getCategories() {
        load();
        return categories;
    }

    public List<GuideArticle> getArticles() {
        load();
        return articles;
    }

    public boolean isAvailable() {
        load();
        return !articles.isEmpty();
    }

    public String getUpdated() {
        load();
        return updated;
    }

    public GuideCategory getCategory(String id) {
        for (GuideCategory c : getCategories()) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public List<GuideArticle> getArticlesByCategory(String categoryId) {
        List<GuideArticle> list = new ArrayList<>();
        for (GuideArticle a : getArticles()) {
            if (a.getCategoryId().equals(categoryId)) {
                list.add(a);
            }
        }
        return list;
    }

    public List<GuideArticle> getArticlesByModel(String businessModel) {
        List<GuideArticle> list = new ArrayList<>();
        for (GuideArticle a : getArticles()) {
            if (a.getTargetModels().isEmpty() || a.getTargetModels().contains(businessModel)) {
                list.add(a);
            }
        }
        return list;
    }

    public GuideArticle getArticleById(String id) {
        if (id == null) {
            return null;
        }
        for (GuideArticle a : getArticles()) {
            if (a.getId().equals(id)) {
                return a;
            }
        }
        return null;
    }

    public GuideArticle getArticleBySlug(String slug) {
        if (slug == null) {
            return null;
        }
        for (GuideArticle a : getArticles()) {
            if (slug.equals(a.getSlug())) {
                return a;
            }
        }
        return null;
    }

    public List<GuideArticle> search(String query, int limit) {
        String q = query == null ? "" : query.length() > MAX_QUERY ? query.substring(0, MAX_QUERY) : query;
        String[] tokens = fold(q).split("[^a-z0-9%/]+");
        final Map<GuideArticle, Integer> scores = new HashMap<>();
        List<GuideArticle> hits = new ArrayList<>();
        for (GuideArticle a : getArticles()) {
            int score = 0;
            String title = fold(a.getTitle());
            String keywords = fold(String.join(" ", a.getKeywords()));
            for (String t : tokens) {
                if (t.length() < 2) {
                    continue;
                }
                if (keywords.contains(t)) {
                    score += 3;
                }
                if (title.contains(t)) {
                    score += 2;
                }
                if (a.getSearchText().contains(t)) {
                    score += 1;
                }
            }
            if (score > 0) {
                scores.put(a, score);
                hits.add(a);
            }
        }
        Collections.sort(hits, new Comparator<GuideArticle>() {
            @Override
            public int compare(GuideArticle x, GuideArticle y) {
                return scores.get(y) - scores.get(x);
            }
        });
        return limit > 0 && hits.size() > limit ? new ArrayList<>(hits.subList(0, limit)) : hits;
    }

    public JsonObject searchForAgent(String query, int limit) {
        List<GuideArticle> hits = search(query, limit);
        JsonObject result = new JsonObject();
        if (hits.isEmpty()) {
            JsonArray topics = new JsonArray();
            for (GuideArticle a : getArticles()) {
                topics.add(new JsonPrimitive(a.getTitle()));
            }
            result.addProperty("note", "No article matched. Ask again with other words. Available topics are listed.");
            result.add("topics", topics);
            return result;
        }
        JsonArray rows = new JsonArray();
        for (GuideArticle a : hits) {
            JsonObject o = new JsonObject();
            o.addProperty("id", a.getId());
            o.addProperty("category_id", a.getCategoryId());
            o.addProperty("title", a.getTitle());
            o.addProperty("summary", a.getSummary());
            o.addProperty("overview", a.getOverview());
            JsonArray steps = new JsonArray();
            for (String s : a.getSteps()) {
                steps.add(new JsonPrimitive(s));
            }
            o.add("steps", steps);
            o.addProperty("open_in_tinky", "Starter Guide, article: " + a.getTitle());
            if (a.isNeedsVerification()) {
                o.addProperty("note", a.getVerifyNote() == null || a.getVerifyNote().isEmpty()
                        ? "Some details are not verified; tell the user to confirm locally or on the marketplace." : a.getVerifyNote());
            }
            JsonArray links = new JsonArray();
            JsonArray sources = new JsonArray();
            for (GuideArticle.Source s : a.getSources()) {
                JsonObject so = new JsonObject();
                so.addProperty("title", s.getTitle());
                so.addProperty("verified", s.isVerified());
                sources.add(so);
                if (s.isVerified() && s.getUrl() != null && s.getUrl().startsWith("https://")) {
                    JsonObject lo = new JsonObject();
                    lo.addProperty("title", s.getTitle());
                    lo.addProperty("url", s.getUrl());
                    links.add(lo);
                }
            }
            o.add("sources", sources);
            if (links.size() > 0) {
                o.add("links", links);
            }
            rows.add(o);
        }
        result.add("rows", rows);
        result.addProperty("knowledge_updated", getUpdated());
        return result;
    }

    public static String fold(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'd').toLowerCase();
    }

    private static synchronized void load() {
        if (articles != null) {
            return;
        }
        List<GuideCategory> cats = new ArrayList<>();
        List<GuideArticle> list = new ArrayList<>();
        try (InputStream in = BusinessDocService.class.getResourceAsStream(RESOURCE)) {
            if (in != null) {
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
                updated = str(root, "last_updated");
                for (JsonElement el : array(root, "categories")) {
                    JsonObject o = el.getAsJsonObject();
                    cats.add(new GuideCategory(str(o, "id"), str(o, "title"), str(o, "description"), str(o, "icon"),
                            o.has("order") && !o.get("order").isJsonNull() ? o.get("order").getAsInt() : 0));
                }
                Collections.sort(cats, new Comparator<GuideCategory>() {
                    @Override
                    public int compare(GuideCategory a, GuideCategory b) {
                        return a.getOrder() - b.getOrder();
                    }
                });
                for (JsonElement el : array(root, "articles")) {
                    list.add(parseArticle(el.getAsJsonObject()));
                }
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Cannot read business_docs.json", e);
        }
        categories = cats;
        articles = list;
    }

    private static GuideArticle parseArticle(JsonObject o) {
        GuideArticle a = new GuideArticle();
        a.setId(str(o, "id"));
        a.setCategoryId(str(o, "category_id"));
        a.setTitle(str(o, "title"));
        String slug = str(o, "slug");
        a.setSlug(slug.isEmpty() ? a.getId() : slug);
        a.setDifficulty(str(o, "difficulty"));
        a.setReadingTime(o.has("reading_time_minutes") && !o.get("reading_time_minutes").isJsonNull() ? o.get("reading_time_minutes").getAsInt() : 0);
        a.setSummary(str(o, "summary"));
        a.setVerifyNote(str(o, "verify_note"));
        strings(o, "target_models", a.getTargetModels());
        strings(o, "keywords", a.getKeywords());
        JsonObject c = o.has("content") && o.get("content").isJsonObject() ? o.getAsJsonObject("content") : new JsonObject();
        a.setOverview(str(c, "overview"));
        a.setWhyItMatters(str(c, "why_it_matters"));
        a.setWhenToDo(str(c, "when_to_do"));
        a.setCosts(str(c, "costs"));
        a.setRealCase(str(c, "real_case"));
        strings(c, "steps", a.getSteps());
        strings(c, "checklist", a.getChecklist());
        strings(c, "common_mistakes", a.getCommonMistakes());
        for (JsonElement f : array(c, "faqs")) {
            JsonObject fo = f.getAsJsonObject();
            a.getFaqs().add(new GuideArticle.Faq(str(fo, "q"), str(fo, "a")));
        }
        for (JsonElement s : array(c, "legal_sources")) {
            JsonObject so = s.getAsJsonObject();
            a.getSources().add(new GuideArticle.Source(str(so, "title"), str(so, "url"),
                    so.has("verified") && !so.get("verified").isJsonNull() && so.get("verified").getAsBoolean()));
        }
        if (o.has("tinky_action") && o.get("tinky_action").isJsonObject()) {
            JsonObject t = o.getAsJsonObject("tinky_action");
            a.setActionLabel(str(t, "label"));
            a.setActionUrl(str(t, "url"));
        }
        StringBuilder text = new StringBuilder();
        text.append(a.getSummary()).append(' ').append(a.getOverview()).append(' ').append(a.getWhyItMatters());
        for (String s : a.getSteps()) {
            text.append(' ').append(s);
        }
        a.setSearchText(fold(text.toString()));
        return a;
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static void strings(JsonObject o, String key, List<String> into) {
        for (JsonElement e : array(o, key)) {
            into.add(e.getAsString());
        }
    }
}
