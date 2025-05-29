package managers.mensajes;

import java.time.LocalDateTime;
import java.util.List;

import managers.mensajes.adjuntos.AdjuntoDTO;

public class Mensaje {
    private Long id;
    private String sender;
    private String asunto;
    private String content;
    private LocalDateTime fechaHora;
    private List<AdjuntoDTO> adjuntos;
    private boolean read;

    public Mensaje() {
    }

    // Constructor original (sin adjuntos)
    public Mensaje(Long id, String sender, String asunto, String content) {
        this.id = id;
        this.sender = sender;
        this.asunto = asunto;
        this.content = content;
    }

    // ► Nuevo constructor con adjuntos
    public Mensaje(Long id, String sender, String asunto, String content, List<AdjuntoDTO> adjuntos, LocalDateTime fechaHora) {
        this.id = id;
        this.sender = sender;
        this.asunto = asunto;
        this.content = content;
        this.adjuntos = adjuntos;
        this.fechaHora = fechaHora;
    }

    // ─── Getters & setters ───────────────────────────────────────────────────

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<AdjuntoDTO> getAdjuntos() {
        return adjuntos;
    }

    public void setAdjuntos(List<AdjuntoDTO> adjuntos) {
        this.adjuntos = adjuntos;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
