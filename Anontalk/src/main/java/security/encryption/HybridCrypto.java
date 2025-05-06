package security.encryption;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Base64;

/**  Cifrado híbrido AES-GCM + RSA-OAEP (SHA-256)  */
public class HybridCrypto {

    public record HybridPayload(String cipherB64, String encKeyB64, String ivB64) { }

    /* ---------- ENCRIPTAR ---------- */
    public static HybridPayload encrypt(String plain, PublicKey destPk) throws Exception {
        // 1) AES
        SecretKey kAES = AESUtils.generateKey();
        byte[] iv = SecureRandom.getInstanceStrong().generateSeed(AESUtils.IV_LEN);
        byte[] cipher = AESUtils.encrypt(plain.getBytes(StandardCharsets.UTF_8), kAES, iv);

        // 2) RSA-OAEP (clave AES)
        byte[] encKey = RSAUtils.encryptRSA(kAES.getEncoded(), destPk);

        return new HybridPayload(
                Base64.getEncoder().encodeToString(cipher),
                Base64.getEncoder().encodeToString(encKey),
                Base64.getEncoder().encodeToString(iv)
        );
    }

    /* ---------- DESCIFRAR ---------- */
    public static String decrypt(HybridPayload p, PrivateKey mySk) throws Exception {
        byte[] encKey = Base64.getDecoder().decode(p.encKeyB64());
        byte[] kBytes = RSAUtils.decryptRSA(encKey, mySk);
        SecretKey kAES = AESUtils.getKeyFromBytes(kBytes);

        byte[] iv = Base64.getDecoder().decode(p.ivB64());
        byte[] cipher = Base64.getDecoder().decode(p.cipherB64());

        byte[] plain = AESUtils.decrypt(cipher, kAES, iv);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
