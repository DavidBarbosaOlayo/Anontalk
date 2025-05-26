package security.passwords;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordRTRepo extends JpaRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUsername(String username);
}

