package rs.ac.ftn.isa.isabackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import rs.ac.ftn.isa.isabackend.dto.ChatMessageDTO;

@Component
public class WebSocketEventListener {

    @Autowired
    private SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String videoId = (String) headerAccessor.getSessionAttributes().get("videoId");

        if (username != null && videoId != null) {
            ChatMessageDTO message = new ChatMessageDTO();
            message.setType(ChatMessageDTO.MessageType.LEAVE);
            message.setSenderUsername(username);
            message.setContent(username + " je napustio/la chat");
            message.setTimestamp(System.currentTimeMillis());

            messagingTemplate.convertAndSend("/topic/chat/" + videoId, message);
        }
    }
}
