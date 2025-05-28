package managers.users;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import security.passwords.ChangeEmailDTO;
import security.passwords.ChangePasswordDTO;
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
        svc.register(dto.getUsername().trim(), dto.getPassword(), dto.getEmail().trim()           // ← PASAMOS email
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Login: si OK 200, si no lanza 401.
     */
    public static record LoginResponse(String salt, String publicKeyBase64, String privateKeyEncryptedBase64,
                                       boolean requirePasswordChange) {
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody UserDTO dto) {
        boolean tokenFlow = svc.authenticate(dto.getUsername().trim(), dto.getPassword());
        var user = repo.findByUsername(dto.getUsername().trim()).get();
        var resp = new LoginResponse(user.getSalt(), user.getPublicKeyBase64(), user.getPrivateKeyEncryptedBase64(),tokenFlow);
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


    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordDTO dto) throws Exception {
        // Ahora usamos dto.getUsername() en lugar de AuthenticationPrincipal
        svc.changePassword(dto.getUsername(), dto.getOldPassword(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-email")
    public ResponseEntity<Void> changeEmail(@RequestBody ChangeEmailDTO dto) {
        svc.changeEmail(dto.getUsername(), dto.getNewEmail());
        return ResponseEntity.ok().build();
    }

    /**
     * Eliminar cuenta
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteAccount(@PathVariable String username) {
        svc.deleteAccount(username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/theme")
    public ResponseEntity<String> getTheme(@PathVariable String username) {
        return ResponseEntity.ok(svc.getTheme(username));
    }

    @PostMapping("/change-theme")
    public ResponseEntity<Void> changeTheme(@RequestBody ChangeThemeDTO dto) {
        svc.changeTheme(dto.getUsername(), dto.getTheme());
        return ResponseEntity.ok().build();
    }

    // DTO para cambio de tema
    public static class ChangeThemeDTO {
        private String username;
        private String theme;
        // getters y setters


        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getTheme() {
            return theme;
        }

        public void setTheme(String theme) {
            this.theme = theme;
        }
    }
}

