package managers.users;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = true)
    private String email;             // ← NUEVO campo

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String salt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "public_key_base64", columnDefinition = "TEXT", nullable = false)
    private String publicKeyBase64;

    @Column(name = "private_key_encrypted_base64", columnDefinition = "TEXT", nullable = false)
    private String privateKeyEncryptedBase64;

    @Column(name = "dark_theme")
    private boolean darkTheme = false;

    // ← getters/setters para todos los campos

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }              // ←
    public void setEmail(String email) { this.email = email; }// ←

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPublicKeyBase64() { return publicKeyBase64; }
    public void setPublicKeyBase64(String publicKeyBase64) { this.publicKeyBase64 = publicKeyBase64; }

    public String getPrivateKeyEncryptedBase64() { return privateKeyEncryptedBase64; }
    public void setPrivateKeyEncryptedBase64(String privateKeyEncryptedBase64) { this.privateKeyEncryptedBase64 = privateKeyEncryptedBase64; }

    public boolean isDarkTheme() { return darkTheme; }
    public void setDarkTheme(boolean darkTheme) { this.darkTheme = darkTheme; }
}
