/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import util.ConfigUtil;

/**
 *
 * @author PC
 */
public class GoogleOAuthService {

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    private final Properties config = ConfigUtil.load("google-oauth.properties");

    public static class GoogleProfile {

        private final String googleId;
        private final String email;
        private final String name;
        private final boolean emailVerified;

        public GoogleProfile(String googleId, String email, String name, boolean emailVerified) {
            this.googleId = googleId;
            this.email = email;
            this.name = name;
            this.emailVerified = emailVerified;
        }

        public String getGoogleId() { return googleId; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public boolean isEmailVerified() { return emailVerified; }
    }

    public String buildLoginUrl(String state) {
        return AUTH_URL
                + "?client_id=" + enc(config.getProperty("client.id"))
                + "&redirect_uri=" + enc(config.getProperty("redirect.uri"))
                + "&response_type=code"
                + "&scope=" + enc("openid email profile")
                + "&state=" + enc(state)
                + "&prompt=select_account";
    }

    public GoogleProfile fetchProfile(String code) throws IOException {
        String tokenBody = "code=" + enc(code)
                + "&client_id=" + enc(config.getProperty("client.id"))
                + "&client_secret=" + enc(config.getProperty("client.secret"))
                + "&redirect_uri=" + enc(config.getProperty("redirect.uri"))
                + "&grant_type=authorization_code";
        JsonObject token = toJson(post(TOKEN_URL, tokenBody));
        String accessToken = token.get("access_token").getAsString();

        JsonObject info = toJson(get(USERINFO_URL, accessToken));
        boolean verified = info.has("email_verified") && info.get("email_verified").getAsBoolean();
        String googleId = info.get("sub").getAsString();
        String email = info.get("email").getAsString().toLowerCase();
        String name = info.has("name") && !info.get("name").isJsonNull() ? info.get("name").getAsString() : "";
        return new GoogleProfile(googleId, email, name, verified);
    }

    private String post(String url, String formBody) throws IOException {
        HttpURLConnection conn = open(url);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        try (OutputStream out = conn.getOutputStream()) {
            out.write(formBody.getBytes("UTF-8"));
        }
        return readResponse(conn);
    }

    private String get(String url, String accessToken) throws IOException {
        HttpURLConnection conn = open(url);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        return readResponse(conn);
    }

    private HttpURLConnection open(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int status = conn.getResponseCode();
        InputStream in = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        if (in == null) {
            throw new IOException("Google returned HTTP " + status + " with no body");
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
        }
        if (status >= 400) {
            throw new IOException("Google returned HTTP " + status + ": " + text);
        }
        return text.toString();
    }

    private JsonObject toJson(String text) {
        return new Gson().fromJson(text, JsonObject.class);
    }

    private static String enc(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
