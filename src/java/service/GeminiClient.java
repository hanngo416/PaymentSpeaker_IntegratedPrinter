package service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import util.ConfigUtil;

public class GeminiClient {

    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final Properties config = ConfigUtil.load("gemini.properties");

    public JsonObject generate(JsonObject body) throws IOException {
        String key = config.getProperty("api.key", "").trim();
        if (key.isEmpty() || key.startsWith("xxxx")) {
            throw new IOException("Gemini api.key is not set");
        }
        String model = config.getProperty("model", "gemini-3.5-flash-lite").trim();
        int readMs = Integer.parseInt(config.getProperty("timeout.seconds", "20").trim()) * 1000;
        URL url = new URL(BASE + model + ":generateContent");
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        try {
            return post(url, key, payload, readMs);
        } catch (RateLimitedException e) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return post(url, key, payload, readMs);
        }
    }

    private JsonObject post(URL url, String key, byte[] payload, int readMs) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setConnectTimeout(5000);
            con.setReadTimeout(readMs);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("x-goog-api-key", key);
            try (OutputStream out = con.getOutputStream()) {
                out.write(payload);
            }
            int status = con.getResponseCode();
            if (status == 429) {
                throw new RateLimitedException();
            }
            if (status != 200) {
                throw new IOException("Gemini returned HTTP " + status);
            }
            return new JsonParser().parse(read(con.getInputStream())).getAsJsonObject();
        } finally {
            con.disconnect();
        }
    }

    private static String read(InputStream in) throws IOException {
        try (InputStream is = in) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) > 0) {
                buf.write(chunk, 0, n);
            }
            return new String(buf.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static class RateLimitedException extends IOException {

        RateLimitedException() {
            super("Gemini returned HTTP 429");
        }
    }
}
