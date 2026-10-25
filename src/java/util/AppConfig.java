/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

/**
 *
 * @author PC
 */
public final class AppConfig {

    private AppConfig() {
    }

    public static final String ORDER_PREFIX = "TINKY";
    public static final int ORDER_MINUTES = 10;
    public static final int ORDER_GRACE_MINUTES = 10;
    public static final long MIN_AMOUNT = 1000L;
    public static final long MAX_AMOUNT = 50000000L;

    public static final String QR_TEMPLATE = "qr_only";

    public static final java.util.TimeZone BANK_ZONE = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
}
