package managers.mensajes.adjuntos;

import jakarta.persistence.*;
import managers.mensajes.MensajeSB;

@Entity
@Table(name = "adjuntos")
public class AdjuntoSB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** relación N-a-1 con el mensaje */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mensaje_id")
    private MensajeSB mensaje;

    private String filename;
    private String mimeType;

    @Column(name = "cipher_b64", columnDefinition = "TEXT", nullable = false)
    private String cipherB64;

    @Column(name = "enc_key_b64", columnDefinition = "TEXT")
    private String encKeyB64;

    @Column(name = "iv_b64", length = 24)
    private String ivB64;

    /* ---------- getters & setters ---------- */
    public Long getId() { return id; }

    public MensajeSB getMensaje() { return mensaje; }
    public void setMensaje(MensajeSB mensaje) { this.mensaje = mensaje; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getCipherB64() { return cipherB64; }
    public void setCipherB64(String cipherB64) { this.cipherB64 = cipherB64; }

    public String getEncKeyB64() { return encKeyB64; }
    public void setEncKeyB64(String encKeyB64) { this.encKeyB64 = encKeyB64; }

    public String getIvB64() { return ivB64; }
    public void setIvB64(String ivB64) { this.ivB64 = ivB64; }
}
