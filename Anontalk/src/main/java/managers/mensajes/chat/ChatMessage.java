package managers.mensajes.chat;

/**
 * DTO para los mensajes que viajan por WebSocket.
 * from    → remitente
 * to      → destinatario
 * content → texto puro del mensaje
 */
public record ChatMessage(String from, String to, String content) {
    // Constructor compacto para validar no-null (opcional)
    public ChatMessage {
        if (from == null || to == null || content == null) {
            throw new IllegalArgumentException("from, to y content no pueden ser null");
        }
    }
}
