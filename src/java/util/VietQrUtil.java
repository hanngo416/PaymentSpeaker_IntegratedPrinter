/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 *
 * @author PC
 */
public final class VietQrUtil {

    private VietQrUtil() {
    }

    public static String buildImageUrl(long amount, String transferContent) {
        return "https://img.vietqr.io/image/"
                + AppConfig.BANK_ID + "-" + AppConfig.ACCOUNT_NO + "-" + AppConfig.QR_TEMPLATE + ".png"
                + "?amount=" + amount
                + "&addInfo=" + encode(transferContent)
                + "&accountName=" + encode(AppConfig.ACCOUNT_NAME);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 luon duoc ho tro", e);
        }
    }
}
