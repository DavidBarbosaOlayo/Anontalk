package managers.users;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import security.passwords.ForgotPasswordDTO;
import security.passwords.ResetPasswordDTO;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService svc;
    private final UserRepo repo;

    public UserController(UserService svc, UserRepo repo) {
        this.svc = svc;
        this.repo = repo;
    }

    /**
     * Registro: si todo OK devuelve 200, si no lanza excepción con su código.
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody UserDTO dto) throws Exception {
        svc.register(
                dto.getUsername().trim(),
                dto.getPassword(),
                dto.getEmail().trim()           // ← PASAMOS email
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Login: si OK 200, si no lanza 401.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UserDTO dto) {
        svc.authenticate(dto.getUsername().trim(), dto.getPassword());
        // Tras autenticar, devolvemos salt + claves en Base64
        var user = repo.findByUsername(dto.getUsername().trim()).get();
        var resp = new LoginResponse(user.getSalt(), user.getPublicKeyBase64(), user.getPrivateKeyEncryptedBase64());
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{username}/publicKey")
    public ResponseEntity<String> publicKey(@PathVariable String username) {
        return repo.findByUsername(username).map(u -> ResponseEntity.ok(u.getPublicKeyBase64())).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordDTO dto) {
        svc.generateResetToken(dto.getEmail().trim());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordDTO dto) throws Exception {
        svc.resetPassword(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    /**
     * DTO para la respuesta de login
     */
    public static record LoginResponse(String salt, String publicKeyBase64, String privateKeyEncryptedBase64) {
    }
}
