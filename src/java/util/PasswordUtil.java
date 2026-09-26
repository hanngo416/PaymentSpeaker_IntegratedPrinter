package util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Băm và kiểm tra mật khẩu bằng PBKDF2 (có sẵn trong JDK, không cần thư viện ngoài).
 * Mật khẩu KHÔNG BAO GIỜ được lưu dạng thô; DB chỉ lưu (salt, hash).
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;   // càng lớn càng chậm với kẻ tấn công dò mật khẩu
    private static final int KEY_BITS = 256;       // 256 bit = 32 byte = 64 ký tự hex
    private static final int SALT_BYTES = 16;      // 16 byte = 32 ký tự hex

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** Sinh salt ngẫu nhiên, trả về dạng chuỗi hex. */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /** Băm mật khẩu với salt cho trước, trả về chuỗi hex. */
    public static String hash(String password, String saltHex) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), fromHex(saltHex), ITERATIONS, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return toHex(factory.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Khong the bam mat khau", e);
        }
    }

    /** So khớp mật khẩu người dùng nhập với hash đã lưu. */
    public static boolean verify(String password, String saltHex, String expectedHashHex) {
        String actual = hash(password, saltHex);
        // isEqual so sánh thời gian cố định, không lộ thông tin qua độ dài thời gian so sánh
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
