package com.itmanagement.servicedesk.modules.tickets.controller;

import com.itmanagement.servicedesk.modules.tickets.model.TicketChatMessageResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketChatSendRequest;
import com.itmanagement.servicedesk.modules.tickets.service.TicketChatService;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
public class TicketChatController {
    private final TicketChatService chat;
    private final SimpMessagingTemplate messaging;

    public TicketChatController(TicketChatService chat, SimpMessagingTemplate messaging) {
        this.chat = chat;
        this.messaging = messaging;
    }

    @MessageMapping("/tickets/{ticketId}/chat")
    public void send(@DestinationVariable Long ticketId, @Valid TicketChatSendRequest request, Principal principal) {
        UserAccount actor = principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserAccount account ? account : null;
        if (actor == null) {
            throw new AccessDeniedException("Sign in to use ticket chat");
        }
        TicketChatMessageResponse saved = chat.send(ticketId, request, actor);
        messaging.convertAndSend("/topic/tickets/" + ticketId + "/chat", saved);
    }
}
