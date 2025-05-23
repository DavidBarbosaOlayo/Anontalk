package managers.mensajes;

public class Mensaje {
    private Long id;
    private String sender;
    private String content;
    private String asunto;

    public Mensaje() { }

    // todo --valorar su uso en un futuro
    public Mensaje(String sender, String content) {
        this.sender = sender;
        this.content = content;

    }

    // Nuevo constructor con id
    public Mensaje(Long id, String sender, String asunto, String content) {
        this.id = id;
        this.sender = sender;
        this.asunto = asunto;
        this.content = content;
    }


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

    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }
}
