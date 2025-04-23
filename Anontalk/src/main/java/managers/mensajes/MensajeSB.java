// src/main/java/managers/mensajes/MensajeSB.java
package managers.mensajes;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
public class MensajeSB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String remitente;
    private String destinatario;

    /**  ────────── NUEVOS CAMPOS ────────── */
    @Column(name = "cipher_text_base64", columnDefinition = "TEXT", nullable = false)
    private String cipherTextBase64;

    @Column(name = "enc_key_base64", columnDefinition = "TEXT", nullable = false)
    private String encKeyBase64;

    @Column(name = "iv_base64", length = 24, nullable = false) // 12 bytes → 16 o 24 B64
    private String ivBase64;
    /**  ─────────────────────────────────── */

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora = LocalDateTime.now();

    public MensajeSB() { }

    public MensajeSB(String remitente,
                     String destinatario,
                     String cipherTextBase64,
                     String encKeyBase64,
                     String ivBase64) {
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.cipherTextBase64 = cipherTextBase64;
        this.encKeyBase64 = encKeyBase64;
        this.ivBase64 = ivBase64;
    }

    /* ─────────── getters / setters ─────────── */

    public Long getId() { return id; }

    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getCipherTextBase64() { return cipherTextBase64; }
    public void setCipherTextBase64(String cipherTextBase64) { this.cipherTextBase64 = cipherTextBase64; }

    public String getEncKeyBase64() { return encKeyBase64; }
    public void setEncKeyBase64(String encKeyBase64) { this.encKeyBase64 = encKeyBase64; }

    public String getIvBase64() { return ivBase64; }
    public void setIvBase64(String ivBase64) { this.ivBase64 = ivBase64; }

    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}
