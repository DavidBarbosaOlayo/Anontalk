package managers.mensajes;

import java.util.List;

import managers.mensajes.adjuntos.AdjuntoDTO;

public class Mensaje {
    private Long id;
    private String sender;
    private String asunto;
    private String content;
    // ► Nuevo:
    private List<AdjuntoDTO> adjuntos;

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
    public Mensaje(Long id, String sender, String asunto, String content, List<AdjuntoDTO> adjuntos) {
        this.id = id;
        this.sender = sender;
        this.asunto = asunto;
        this.content = content;
        this.adjuntos = adjuntos;
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
}
