package managers.users;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import security.encryption.AESUtils;
import security.encryption.RSAUtils;
import security.passwords.PasswordRTRepo;
import security.passwords.PasswordResetToken;
import security.passwords.PasswordsUtils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional
public class UserService {
    private final UserRepo repo;
    private final PasswordRTRepo tokenRepo;
    private final JavaMailSender mailSender;

    // Regex: mínimo 8 caracteres, 1 mayúscula, 1 minúscula, 1 dígito y 1 carácter especial
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Autowired
    public UserService(UserRepo repo, PasswordRTRepo tokenRepo, JavaMailSender mailSender) {
        this.repo = repo;
        this.tokenRepo = tokenRepo;
        this.mailSender = mailSender;
    }

    /**
     * Registro de un nuevo usuario.
     */
    public void register(String username, String plainPwd, String email) throws Exception {
        // Validaciones básicas
        if (username == null || username.length() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The username must be at least 5 characters long");
        }
        if (plainPwd == null || !PASSWORD_PATTERN.matcher(plainPwd).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The password must be at least 8 characters long, and include an uppercase letter, a lowercase letter, a number, and a special character");
        }
        if (repo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that username already exists");
        }

        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email address");
        }
        if (repo.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with that email already exists");
        }

        // Generar par RSA
        KeyPair kp = RSAUtils.generateKeyPair();
        String pubB64 = RSAUtils.toBase64(kp.getPublic());
        String privB64 = RSAUtils.toBase64(kp.getPrivate());

        // Crear salt y hash de la contraseña
        String salt = UUID.randomUUID().toString();
        String hash = PasswordsUtils.hashPassword(plainPwd, salt);

        // Derivar clave AES y cifrar la clave privada RSA
        SecretKey aesKey = deriveAesKeyFromPassword(plainPwd, salt);
        String privEnc = AESUtils.encrypt(privB64, aesKey);

        // Persistir usuario
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setSalt(salt);
        u.setPasswordHash(hash);
        u.setPublicKeyBase64(pubB64);
        u.setPrivateKeyEncryptedBase64(privEnc);
        repo.save(u);
    }

    /**
     * Autenticación de usuario existente.
     */
    public boolean authenticate(String username, String plainPwd) {
        var opt = repo.findByUsername(username);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
        }
        User user = opt.get();

        // 1) Login normal
        if (PasswordsUtils.verifyPassword(plainPwd, user.getSalt(), user.getPasswordHash())) {
            return false;    // no es flujo token
        }

        // 2) Login con token
        var optToken = tokenRepo.findByToken(plainPwd);
        if (optToken.isPresent() && !optToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            try {
                // resetea la contraseña internamente al token
                resetPassword(plainPwd, plainPwd);
                return true;  // flujo token
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado");
            }
        }

        // 3) si no coincide ni con pwd ni con token:
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }


    /**
     * Genera un token de recuperación, lo guarda y envía un correo.
     */
    // dentro de UserService.java
    public void generateResetToken(String email) {
        // 1) Buscar por email
        var opt = repo.findByEmail(email);
        if (opt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Email no registrado");
        }

        // 2) Eliminar tokens anteriores
        tokenRepo.deleteByUsername(opt.get().getUsername());

        // 3) Crear y guardar nuevo token
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken(token);
        prt.setUsername(opt.get().getUsername());
        prt.setExpiryDate(LocalDateTime.now().plusHours(1));
        tokenRepo.save(prt);

        // 4) Enviar correo A LA DIRECCIÓN que vino
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Anontalk: Tu contraseña temporal");
        msg.setText("Hola,\n\n" + "Tu contraseña temporal es:\n\n" + token + "\n\n" + "Cópiala y úsala para iniciar sesión. " + "Después podrás definir tu nueva contraseña desde tu perfil.\n\n" + "Este código expirará en 1 hora.");
        mailSender.send(msg);
    }

    /**
     * Valida el token y realiza el reseteo de contraseña + regeneración de claves RSA.
     */
    public void resetPassword(String token, String newPassword) throws Exception {
        var optToken = tokenRepo.findByToken(token);
        if (optToken.isEmpty() || optToken.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido o expirado");
        }

        var username = optToken.get().getUsername();
        User u = repo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // ← Solo validamos patrón si no es el flujo interno de token-based reset
        boolean isTokenFlow = token.equals(newPassword);
        if (!isTokenFlow && !PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener: 8+ caracteres, mayúscula, minúscula, número y carácter especial");
        }

        // Generar nuevo salt + hash (token en este caso funciona como pwd)
        String salt = UUID.randomUUID().toString();
        String hash = PasswordsUtils.hashPassword(newPassword, salt);

        // Derivar AES y generar nuevo par RSA
        SecretKey aesKey = deriveAesKeyFromPassword(newPassword, salt);
        KeyPair kp = RSAUtils.generateKeyPair();
        String pubB64 = RSAUtils.toBase64(kp.getPublic());
        String privB64 = RSAUtils.toBase64(kp.getPrivate());
        String privEnc = AESUtils.encrypt(privB64, aesKey);

        // Actualizar usuario
        u.setSalt(salt);
        u.setPasswordHash(hash);
        u.setPublicKeyBase64(pubB64);
        u.setPrivateKeyEncryptedBase64(privEnc);
        repo.save(u);

        // Invalidar token
        tokenRepo.delete(optToken.get());
    }

    /**
     * Deriva una clave AES a partir de la contraseña y el salt.
     */
    private SecretKey deriveAesKeyFromPassword(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            return AESUtils.getKeyFromBytes(keyBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error al derivar clave AES: " + e.getMessage(), e);
        }
    }

    /**
     * Permite al usuario autenticado cambiar su propia contraseña.
     */
    public void changePassword(String username, String oldPwd, String newPwd) throws Exception {
        // 1) Validar formato de nueva contraseña
        if (!PASSWORD_PATTERN.matcher(newPwd).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial");
        }

        // 2) Cargar usuario
        User u = repo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // 3) Verificar contraseña actual
        if (!PasswordsUtils.verifyPassword(oldPwd, u.getSalt(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Contraseña actual incorrecta");
        }

        // 4) Desencriptar privateKey con la clave derivada de oldPwd
        SecretKey oldKey = deriveAesKeyFromPassword(oldPwd, u.getSalt());
        String privB64 = AESUtils.decrypt(u.getPrivateKeyEncryptedBase64(), oldKey);

        // 5) Generar nuevo salt + hash
        String newSalt = UUID.randomUUID().toString();
        String newHash = PasswordsUtils.hashPassword(newPwd, newSalt);

        // 6) Encriptar la misma privateKey con la nueva clave derivada de newPwd
        SecretKey newKey = deriveAesKeyFromPassword(newPwd, newSalt);
        String newPrivEnc = AESUtils.encrypt(privB64, newKey);

        // 7) Guardar cambios
        u.setSalt(newSalt);
        u.setPasswordHash(newHash);
        u.setPrivateKeyEncryptedBase64(newPrivEnc);
        repo.save(u);
    }

    // dentro de UserService.java
    public void changeEmail(String username, String newEmail) {
        if (newEmail == null || !EMAIL_PATTERN.matcher(newEmail).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email no válido");
        }
        if (repo.findByEmail(newEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email ya registrado");
        }
        User u = repo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        u.setEmail(newEmail);
        repo.save(u);
    }

    public void deleteAccount(String username) {
        User u = repo.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        repo.delete(u);
    }

}
