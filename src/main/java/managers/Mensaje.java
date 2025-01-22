package managers;

public class Mensaje {
    private String sender;
    private String content;
    private boolean leido;

    public Mensaje(String sender, String content) {
        this.sender = sender.substring(1);
        this.content = content;
        this.leido = false; // Inicialmente no leído
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }
}
