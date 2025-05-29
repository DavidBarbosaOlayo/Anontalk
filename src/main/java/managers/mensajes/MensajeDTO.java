package managers.mensajes;

import managers.mensajes.adjuntos.AdjuntoDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MensajeDTO {
    private Long id;
    private String remitente;
    private String destinatario;
    private String asunto;
    private String cipherTextBase64;
    private String encKeyBase64;
    private String ivBase64;
    private LocalDateTime fechaHora;
    private List<AdjuntoDTO> adjuntos = new ArrayList<>();
    private boolean read;
    private LocalDateTime expiryDate; // Nuevo campo para el temporizador


    public MensajeDTO() {
    }

    public MensajeDTO(Long id, String remitente, String destinatario, String asunto, String cipherTextBase64, String encKeyBase64, String ivBase64, LocalDateTime fechaHora, LocalDateTime expiryDate) {
        this.id = id;
        this.remitente = remitente;
        this.destinatario = destinatario;
        this.asunto = asunto;
        this.cipherTextBase64 = cipherTextBase64;
        this.encKeyBase64 = encKeyBase64;
        this.ivBase64 = ivBase64;
        this.fechaHora = fechaHora;
        this.expiryDate = expiryDate;
    }

    // ─── Getters & setters ─────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
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

    public List<AdjuntoDTO> getAdjuntos() {
        return adjuntos;
    }

    public void setAdjuntos(List<AdjuntoDTO> adjuntos) {
        this.adjuntos = adjuntos;
    }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }
}
