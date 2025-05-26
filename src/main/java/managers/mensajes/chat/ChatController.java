package managers.mensajes.chat;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Controlador STOMP para reenviar ChatMessage desde
 * /app/chat.send → /topic/chat.{destinatario}
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate broker;

    public ChatController(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    /**
     * Cuando un cliente envía a destino "/app/chat.send",
     * este método recibe el ChatMessage y lo repubblica
     * en "/topic/chat.{to}".
     */
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessage msg) {
        // msg.from(), msg.to(), msg.content()
        broker.convertAndSend(
                "/topic/chat." + msg.to(),
                msg
        );
    }
}
