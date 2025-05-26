package security.passwords;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordsUtils {

    // Parámetros de PBKDF2
    private static final int ITERATIONS = 65536;   // Nº de iteraciones
    private static final int KEY_LENGTH = 256;     // Longitud de la clave derivada en bits
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * Hashea una contraseña con PBKDF2.
     *
     * @param password Contraseña en texto plano
     * @param salt     Un salt aleatorio para cada usuario
     * @return hash en Base64
     */
    public static String hashPassword(String password, String salt) {
        char[] chars = password.toCharArray();
        byte[] saltBytes = salt.getBytes();

        PBEKeySpec spec = new PBEKeySpec(chars, saltBytes, ITERATIONS, KEY_LENGTH);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al hashear la contraseña: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifica si una contraseña en texto plano corresponde al hash almacenado (con el mismo salt).
     *
     * @param plainPassword Contraseña en texto plano
     * @param salt          Salt del usuario
     * @param expectedHash  Hash esperado (en BD)
     * @return true si coinciden, false en caso contrario
     */
    public static boolean verifyPassword(String plainPassword, String salt, String expectedHash) {
        String newHash = hashPassword(plainPassword, salt);
        return newHash.equals(expectedHash);
    }
}
