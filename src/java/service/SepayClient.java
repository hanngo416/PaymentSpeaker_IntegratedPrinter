/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import util.AppConfig;
import util.ConfigUtil;

/**
 *
 * @author PC
 */
public class SepayClient {

    private static final String LIST_URL = "https://my.sepay.vn/userapi/transactions/list";

    private static final int PAGE_LIMIT = 1000;

    private final Properties config = ConfigUtil.load("sepay.properties");

    public int getWindowMinutes() {
        return Integer.parseInt(config.getProperty("window.minutes", "30").trim());
    }

    public List<Map<String, String>> fetchSince(Date since) throws IOException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(AppConfig.BANK_ZONE);
        String from = format.format(since);

        String url = LIST_URL
                + "?account_number=" + enc(config.getProperty("account.number").trim())
                + "&transaction_date_min=" + enc(from)
                + "&limit=" + PAGE_LIMIT;

        JsonObject body = new JsonParser().parse(get(url)).getAsJsonObject();
        List<Map<String, String>> result = new ArrayList<>();
        if (!body.has("transactions") || !body.get("transactions").isJsonArray()) {
            return result;
        }
        JsonArray items = body.getAsJsonArray("transactions");
        for (JsonElement element : items) {
            if (element.isJsonObject()) {
                result.add(toPayload(element.getAsJsonObject()));
            }
        }
        Collections.sort(result, new Comparator<Map<String, String>>() {
            @Override
            public int compare(Map<String, String> a, Map<String, String> b) {
                int byTime = String.valueOf(a.get("transactionDate")).compareTo(String.valueOf(b.get("transactionDate")));
                return byTime != 0 ? byTime : Long.compare(idOf(a), idOf(b));
            }
        });
        return result;
    }

    private static long idOf(Map<String, String> payload) {
        try {
            return Long.parseLong(payload.get("id"));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private Map<String, String> toPayload(JsonObject t) {
        BigDecimal amountIn = number(text(t, "amount_in"));
        Map<String, String> p = new LinkedHashMap<>();
        p.put("id", text(t, "id"));
        p.put("transferType", amountIn.signum() > 0 ? "in" : "out");
        p.put("transferAmount", amountIn.signum() > 0 ? amountIn.toPlainString() : number(text(t, "amount_out")).toPlainString());
        p.put("content", text(t, "transaction_content"));
        p.put("accountNumber", text(t, "account_number"));
        p.put("gateway", text(t, "bank_brand_name"));
        p.put("transactionDate", text(t, "transaction_date"));
        return p;
    }

    private static String text(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return (e == null || e.isJsonNull()) ? null : e.getAsString();
    }

    private static BigDecimal number(String raw) {
        try {
            return raw == null ? BigDecimal.ZERO : new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + config.getProperty("api.token").trim());
        conn.setRequestProperty("Content-Type", "application/json");

        int status = conn.getResponseCode();
        InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) {
            throw new IOException("SePay returned HTTP " + status + " with no body");
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        if (status >= 400) {
            throw new IOException("SePay returned HTTP " + status);
        }
        return text.toString();
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
