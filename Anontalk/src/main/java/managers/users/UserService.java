// src/main/java/managers/users/UserService.java
package managers.users;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import security.AESUtils;
import security.PasswordsUtils;
import security.RSAUtils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.KeyPair;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepo repo;

    // Regex: mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 dígito y 1 carácter especial
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    public UserService(UserRepo repo) {
        this.repo = repo;
    }

    public void register(String username, String plainPwd) throws Exception {
        // — Validaciones básicas —
        if (username == null || username.length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario debe tener al menos 5 caracteres");
        }
        if (plainPwd == null || !PASSWORD_PATTERN.matcher(plainPwd).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial");
        }
        if (repo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre");
        }

        // — Generar par RSA y serializar a Base64 —
        KeyPair kp = RSAUtils.generateKeyPair();
        String pubB64 = RSAUtils.toBase64(kp.getPublic());
        String privB64 = RSAUtils.toBase64(kp.getPrivate());

        // — Crear salt y hash de la contraseña —
        String salt = UUID.randomUUID().toString();
        String hash = PasswordsUtils.hashPassword(plainPwd, salt);

        // — Derivar clave AES de la contraseña + salt (PBKDF2) —
        SecretKey aesKey = deriveAesKeyFromPassword(plainPwd, salt);

        // — Cifrar la clave privada RSA con la clave AES —
        String privEnc = AESUtils.encrypt(privB64, aesKey);
        // — Persistir nuevo usuario —
        User u = new User();
        u.setUsername(username);
        u.setSalt(salt);
        u.setPasswordHash(hash);
        u.setPublicKeyBase64(pubB64);
        u.setPrivateKeyEncryptedBase64(privEnc);
        repo.save(u);
    }

    public void authenticate(String username, String plainPwd) {
        var opt = repo.findByUsername(username);
        if (opt.isEmpty() || !PasswordsUtils.verifyPassword(plainPwd, opt.get().getSalt(), opt.get().getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
    }

    /**
     * Deriva una clave AES a partir de la contraseña y el salt, usando PBKDF2.
     */
    private SecretKey deriveAesKeyFromPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536,       // iteraciones
                    256          // longitud en bits
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return AESUtils.getKeyFromBytes(keyBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al derivar clave AES: " + e.getMessage(), e);
        }
    }
}
