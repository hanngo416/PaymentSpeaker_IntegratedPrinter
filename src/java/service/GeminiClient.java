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
        String main = config.getProperty("model", "gemini-3.6-flash").trim();
        String backup = config.getProperty("fallback.model", "gemini-3.5-flash-lite").trim();
        int readMs = Integer.parseInt(config.getProperty("timeout.seconds", "20").trim()) * 1000;
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        IOException last = null;
        String[] models = backup.isEmpty() || backup.equals(main) ? new String[]{main} : new String[]{main, backup};
        for (String model : models) {
            URL url = new URL(BASE + model + ":generateContent");
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    return post(url, key, payload, readMs);
                } catch (RateLimitedException e) {
                    last = e;
                    if (attempt == 0) {
                        pause();
                    }
                }
            }
        }
        throw last;
    }

    private static void pause() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
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
            if (status == 429 || status == 500 || status == 503) {
                throw new RateLimitedException(status);
            }
            if (status != 200) {
                String detail = "";
                try {
                    detail = " " + read(con.getErrorStream());
                } catch (IOException | RuntimeException ignored) {
                }
                throw new IOException("Gemini returned HTTP " + status + detail);
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

        RateLimitedException(int status) {
            super("Gemini returned HTTP " + status);
        }
    }
}
