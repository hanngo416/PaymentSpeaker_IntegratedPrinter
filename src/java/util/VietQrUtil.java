/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

/**
 *
 * @author PC
 */
public final class VietQrUtil {

    // The same sepay.properties the polling job reads, so the QR and the polling can never point at two different accounts
    private static final Properties CONFIG = ConfigUtil.load("sepay.properties");

    private VietQrUtil() {
    }

    public static String buildImageUrl(long amount, String transferContent) {
        return "https://img.vietqr.io/image/"
                + setting("bank.id") + "-" + setting("account.number") + "-" + AppConfig.QR_TEMPLATE + ".png"
                + "?amount=" + amount
                + "&addInfo=" + encode(transferContent)
                + "&accountName=" + encode(setting("account.name"));
    }

    private static String setting(String key) {
        String value = CONFIG.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(key + " is missing in sepay.properties");
        }
        return value.trim();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 luon duoc ho tro", e);
        }
    }
}
