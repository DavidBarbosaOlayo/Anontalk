package managers.mensajes;

import jakarta.persistence.*;
import managers.mensajes.adjuntos.AdjuntoSB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "mensajes")
public class MensajeSB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String remitente;
    private String destinatario;

    @Column(nullable = true)
    private String asunto;

    @Column(name = "cipher_text_base64", columnDefinition = "TEXT", nullable = false)
    private String cipherTextBase64;

    @Column(name = "enc_key_base64", columnDefinition = "TEXT", nullable = true)
    private String encKeyBase64;

    @Column(name = "iv_base64", length = 24, nullable = true) // 12 bytes → 16 o 24 B64
    private String ivBase64;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora = LocalDateTime.now();


    @OneToMany(mappedBy = "mensaje", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AdjuntoSB> adjuntos = new ArrayList<>();

    public MensajeSB() {
    }

    public MensajeSB(String remitente, String destinatario, String cipherTextBase64, String encKeyBase64, String ivBase64) {
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.cipherTextBase64 = cipherTextBase64;
        this.encKeyBase64 = encKeyBase64;
        this.ivBase64 = ivBase64;
    }

    public Long getId() {
        return id;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getRemitente() {
        return remitente;
    }

    public void setRemitente(String remitente) {
        this.remitente = remitente;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getCipherTextBase64() {
        return cipherTextBase64;
    }

    public void setCipherTextBase64(String cipherTextBase64) {
        this.cipherTextBase64 = cipherTextBase64;
    }

    public String getEncKeyBase64() {
        return encKeyBase64;
    }

    public void setEncKeyBase64(String encKeyBase64) {
        this.encKeyBase64 = encKeyBase64;
    }

    public String getIvBase64() {
        return ivBase64;
    }

    public void setIvBase64(String ivBase64) {
        this.ivBase64 = ivBase64;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getAsunto() {
        return asunto;
    }
}
