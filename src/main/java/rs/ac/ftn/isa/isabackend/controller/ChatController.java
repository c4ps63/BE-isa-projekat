package rs.ac.ftn.isa.isabackend.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import rs.ac.ftn.isa.isabackend.dto.ChatMessageDTO;
import rs.ac.ftn.isa.isabackend.model.User;

import java.security.Principal;

@Controller
public class ChatController {

    @MessageMapping("/chat.sendMessage/{videoId}")
    @SendTo("/topic/chat/{videoId}")
    public ChatMessageDTO sendMessage(@DestinationVariable String videoId,
                                      ChatMessageDTO message,
                                      Principal principal) {
        User user = extractUser(principal);
        message.setType(ChatMessageDTO.MessageType.CHAT);
        message.setSenderUsername(user.getUsername());
        message.setSenderAvatarUrl(user.getAvatarUrl());
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    @MessageMapping("/chat.join/{videoId}")
    @SendTo("/topic/chat/{videoId}")
    public ChatMessageDTO joinChat(@DestinationVariable String videoId,
                                   ChatMessageDTO message,
                                   SimpMessageHeaderAccessor headerAccessor,
                                   Principal principal) {
        User user = extractUser(principal);
        headerAccessor.getSessionAttributes().put("username", user.getUsername());
        headerAccessor.getSessionAttributes().put("videoId", videoId);

        message.setType(ChatMessageDTO.MessageType.JOIN);
        message.setSenderUsername(user.getUsername());
        message.setSenderAvatarUrl(user.getAvatarUrl());
        message.setTimestamp(System.currentTimeMillis());
        message.setContent(user.getUsername() + " se pridružio/la chatu");
        return message;
    }

    private User extractUser(Principal principal) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) principal;
        return (User) auth.getPrincipal();
    }
}
