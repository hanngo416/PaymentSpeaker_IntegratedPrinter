package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * The hash of one audit record: SHA-256 of the previous hash plus every field of this record.
 * Changing any field of any old record changes its hash and breaks every record after it.
 */
public final class AuditHash {

    public static final String GENESIS_PREV = "0000000000000000000000000000000000000000000000000000000000000000";

    private AuditHash() {
    }

    public static String compute(String prevHash, int logId, String actor, String action, String entity,
            String entityId, String detail, Timestamp createdAt) {
        // DATETIME2(3) keeps milliseconds, so the text is the same when the row is read back
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(createdAt);
        String text = prevHash + "|" + logId + "|" + actor + "|" + action + "|" + entity + "|" + entityId
                + "|" + (detail == null ? "" : detail) + "|" + time;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
