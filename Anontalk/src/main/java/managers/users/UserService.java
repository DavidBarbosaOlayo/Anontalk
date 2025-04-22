package managers.users;

import org.springframework.stereotype.Service;
import security.PasswordsUtils;

@Service
public class UserService {
    private final UserRepo repo;

    public UserService(UserRepo repo) {
        this.repo = repo;
    }

    public boolean register(String username, String plainPwd) {
        if (repo.findByUsername(username).isPresent()) return false;
        String salt = java.util.UUID.randomUUID().toString();
        String hash = PasswordsUtils.hashPassword(plainPwd, salt);
        User u = new User();
        u.setUsername(username);
        u.setSalt(salt);
        u.setPasswordHash(hash);
        repo.save(u);
        return true;
    }

    public boolean authenticate(String username, String plainPwd) {
        return repo.findByUsername(username).map(u -> PasswordsUtils.verifyPassword(plainPwd, u.getSalt(), u.getPasswordHash())).orElse(false);
    }
}
