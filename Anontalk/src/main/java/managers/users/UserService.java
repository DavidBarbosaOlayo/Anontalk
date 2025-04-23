// src/main/java/managers/users/UserService.java
package managers.users;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import security.PasswordsUtils;

import java.util.regex.Pattern;

@Service
public class UserService {
    private final UserRepo repo;

    // Regex: mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 dígito y 1 carácter especial
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    public UserService(UserRepo repo) {
        this.repo = repo;
    }

    public void register(String username, String plainPwd) {
        // Validaciones
        if (username == null || username.length() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario debe tener al menos 5 caracteres"
            );
        }
        if (plainPwd == null || !PASSWORD_PATTERN.matcher(plainPwd).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres, " +
                            "una mayúscula, una minúscula, un número y un carácter especial"
            );
        }
        if (repo.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese nombre"
            );
        }

        // Guardar
        String salt = java.util.UUID.randomUUID().toString();
        String hash = PasswordsUtils.hashPassword(plainPwd, salt);

        User u = new User();
        u.setUsername(username);
        u.setSalt(salt);
        u.setPasswordHash(hash);
        repo.save(u);
    }

    public void authenticate(String username, String plainPwd) {
        var optUser = repo.findByUsername(username);
        if (optUser.isEmpty() ||
                !PasswordsUtils.verifyPassword(plainPwd, optUser.get().getSalt(), optUser.get().getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuario o contraseña incorrectos"
            );
        }
        // si llega aquí, está OK
    }
}
