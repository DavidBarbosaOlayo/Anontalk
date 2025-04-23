// src/main/java/managers/users/UserController.java
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
        svc.register(dto.getUsername().trim(), dto.getPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody UserDTO dto) {
        svc.authenticate(dto.getUsername().trim(), dto.getPassword());
        return ResponseEntity.ok().build();
    }
}
