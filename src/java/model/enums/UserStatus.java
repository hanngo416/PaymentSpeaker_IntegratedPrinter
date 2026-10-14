/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package model.enums;

/**
 *
 * @author PC
 */
public enum UserStatus {
    ACTIVE,
    LOCKED,
    PENDING   // signed in with Google but no account yet: waits for an admin to give a role
}
