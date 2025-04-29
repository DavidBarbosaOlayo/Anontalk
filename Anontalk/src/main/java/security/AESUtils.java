package security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public class AESUtils {

    public static final int KEY_LEN_BITS = 256;
    public static final int IV_LEN = 12;          // 96 bits recomendado GCM
    private static final int TAG_LEN = 128;       // 16 bytes
    private static final String TRANSFORM = "AES/GCM/NoPadding";

    /* ---------- generación / conversión ---------- */

    public static SecretKey generateKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(KEY_LEN_BITS);
        return kg.generateKey();
    }

    public static SecretKey getKeyFromBytes(byte[] bytes) {
        return new SecretKeySpec(bytes, "AES");
    }

    /* =============  STRING helpers (iv + cipher pegados) ============= */

    public static String encrypt(String plain, SecretKey key) throws Exception {
        byte[] iv = SecureRandom.getInstanceStrong().generateSeed(IV_LEN);
        byte[] cipher = encrypt(plain.getBytes(StandardCharsets.UTF_8), key, iv);
        byte[] ivPlus = ByteBuffer.allocate(iv.length + cipher.length).put(iv).put(cipher).array();
        return Base64.getEncoder().encodeToString(ivPlus);
    }

    public static String decrypt(String base64, SecretKey key) throws Exception {
        byte[] ivPlus = Base64.getDecoder().decode(base64);
        byte[] iv = Arrays.copyOfRange(ivPlus, 0, IV_LEN);
        byte[] cipher = Arrays.copyOfRange(ivPlus, IV_LEN, ivPlus.length);
        byte[] plain = decrypt(cipher, key, iv);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /* =============  BYTES (iv separado) ============= */

    public static byte[] encrypt(byte[] plain, SecretKey key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN, iv));
        return c.doFinal(plain);                // cipher || tag
    }

    public static byte[] decrypt(byte[] cipher, SecretKey key, byte[] iv) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN, iv));
        return c.doFinal(cipher);
    }
}
