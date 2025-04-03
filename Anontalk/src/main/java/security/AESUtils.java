package security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class AESUtils {

    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // AES de 256 bits
        return keyGen.generateKey();
    }

    // Si ya tienes la clave en byte[], puedes reconstruir el SecretKey
    public static SecretKey getKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, "AES");
    }

    // Cifrar en Base64 para guardarlo en la BD
    public static String encrypt(String data, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // Descifrar desde Base64
    public static String decrypt(String encryptedData, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, "UTF-8");
    }

    // Ej. método para crear la clave una sola vez y guardarla
    public static void createAndStoreKeyIfNotExist() {
        try {
            File f = new File("aes.key");
            if (!f.exists()) {
                SecretKey key = AESUtils.generateKey();
                // Guardar en Base64 para texto
                String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
                Files.write(f.toPath(), encodedKey.getBytes(StandardCharsets.UTF_8));
                System.out.println("Clave AES generada y almacenada en aes.key");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Ej. método para cargar la clave en memoria
    public static SecretKey loadKey() {
        try {
            File f = new File("aes.key");
            if (!f.exists()) {
                throw new FileNotFoundException("No se encontró el fichero aes.key");
            }
            String encodedKey = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
            return AESUtils.getKeyFromBytes(decodedKey);
        } catch (Exception e) {
            throw new RuntimeException("Error al cargar la clave AES.", e);
        }
    }
}
