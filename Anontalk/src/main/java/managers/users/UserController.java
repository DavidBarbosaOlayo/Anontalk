package managers.users;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService svc;

    public UserController(UserService svc) {
        this.svc = svc;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody UserDTO dto) {
        boolean ok = svc.register(dto.getUsername(), dto.getPassword());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(409).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody UserDTO dto) {
        boolean ok = svc.authenticate(dto.getUsername(), dto.getPassword());
        return ok ? ResponseEntity.ok().build() : ResponseEntity.status(401).build();
    }
}
