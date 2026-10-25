/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.sql.Date;

/**
 *
 * @author PC
 */
public final class Params {

    private Params() {
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static Integer parseInt(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Date parseDate(String s) {
        try {
            return s == null || s.trim().isEmpty() ? null : Date.valueOf(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
