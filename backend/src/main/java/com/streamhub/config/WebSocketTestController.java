package com.streamhub.config;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
//temporary test controller verifying websocket ping pong round trip
public class WebSocketTestController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketTestController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/ping")
    public void handlePing(Principal principal) {
        if (principal != null) {
            messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/pong", "pong");
        }
    }
}
