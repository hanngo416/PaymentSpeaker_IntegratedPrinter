/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.sql.SQLException;

/**
 *
 * @author PC
 */
public final class SqlErrors {

    private SqlErrors() {
    }

    public static boolean isDuplicateKey(SQLException e) {
        return e.getErrorCode() == 2627 || e.getErrorCode() == 2601;
    }
}
