/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes and verifies passwords with PBKDF2 (built into the JDK, no external library).
 * A password is NEVER stored as plain text; the DB only stores (salt, hash).
 *
 * @author PC
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;   // more iterations = slower for an attacker guessing passwords
    private static final int KEY_BITS = 256;       // 256 bits = 32 bytes = 64 hex characters
    private static final int SALT_BYTES = 16;      // 16 bytes = 32 hex characters

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** Generates a random salt and returns it as a hex string. */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /** Hashes a password with the given salt and returns the result as a hex string. */
    public static String hash(String password, String saltHex) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(saltHex), ITERATIONS, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return toHex(factory.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot hash password", e);
        }
    }

    /** Checks the password the user typed against the stored hash. */
    public static boolean verify(String password, String saltHex, String expectedHashHex) {
        String actual = hash(password, saltHex);
        // isEqual compares in constant time, so the comparison time leaks nothing about the hash
        return MessageDigest.isEqual(actual.getBytes(), expectedHashHex.getBytes());
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
