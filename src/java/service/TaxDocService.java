package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaxDocService {

    public static final int MAX_RESULTS = 3;

    private static class Doc {

        JsonObject json;
        String keywords;
        String title;
        String text;
        int score;
    }

    private static List<Doc> docs;
    private static String checked = "";

    public JsonObject search(String query) {
        List<Doc> all = load();
        String[] tokens = fold(query == null ? "" : query).split("[^a-z0-9%/]+");
        List<Doc> hits = new ArrayList<>();
        for (Doc d : all) {
            d.score = 0;
            for (String t : tokens) {
                if (t.length() < 2) {
                    continue;
                }
                if (d.keywords.contains(t)) {
                    d.score += 3;
                }
                if (d.title.contains(t)) {
                    d.score += 2;
                }
                if (d.text.contains(t)) {
                    d.score += 1;
                }
            }
            if (d.score > 0) {
                hits.add(d);
            }
        }
        Collections.sort(hits, new Comparator<Doc>() {
            @Override
            public int compare(Doc a, Doc b) {
                return b.score - a.score;
            }
        });
        JsonArray rows = new JsonArray();
        JsonObject result = new JsonObject();
        if (hits.isEmpty()) {
            JsonArray topics = new JsonArray();
            for (Doc d : all) {
                topics.add(d.json.get("title"));
            }
            result.addProperty("note", "No document matched. Ask again with other words. Available topics are listed.");
            result.add("topics", topics);
        } else {
            for (int i = 0; i < hits.size() && i < MAX_RESULTS; i++) {
                rows.add(hits.get(i).json);
            }
            result.add("rows", rows);
        }
        result.addProperty("knowledge_checked_on", checked);
        return result;
    }

    private static synchronized List<Doc> load() {
        if (docs != null) {
            return docs;
        }
        List<Doc> list = new ArrayList<>();
        try (InputStream in = TaxDocService.class.getResourceAsStream("/taxdocs.json")) {
            if (in == null) {
                throw new IllegalStateException("taxdocs.json not found in src/java");
            }
            Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            checked = root.get("checked").getAsString();
            for (JsonElement el : root.getAsJsonArray("docs")) {
                JsonObject o = el.getAsJsonObject();
                Doc d = new Doc();
                d.json = new JsonObject();
                d.json.add("title", o.get("title"));
                d.json.add("text", o.get("text"));
                d.json.add("source", o.get("source"));
                if (o.has("links")) {
                    d.json.add("links", o.get("links"));
                }
                if (o.has("note") && !o.get("note").getAsString().isEmpty()) {
                    d.json.add("note", o.get("note"));
                }
                StringBuilder kw = new StringBuilder();
                for (JsonElement k : o.getAsJsonArray("keywords")) {
                    kw.append(fold(k.getAsString())).append(' ');
                }
                d.keywords = kw.toString();
                d.title = fold(o.get("title").getAsString());
                d.text = fold(o.get("text").getAsString());
                list.add(d);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read taxdocs.json", e);
        }
        docs = list;
        return docs;
    }

    static String fold(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "").replace('đ', 'd');
    }
}
