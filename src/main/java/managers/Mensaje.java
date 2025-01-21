package managers;

public class Mensaje {
    private String sender;
    private String content;

    public Mensaje(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }
}
