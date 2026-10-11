package com.itmanagement.servicedesk.modules.tickets.service;

import com.itmanagement.servicedesk.modules.audit.service.AuditService;
import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import com.itmanagement.servicedesk.modules.tickets.model.TicketChatMessage;
import com.itmanagement.servicedesk.modules.tickets.model.TicketChatMessageResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketChatSendRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketStatus;
import com.itmanagement.servicedesk.modules.tickets.model.TicketUserSummary;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketChatMessageRepository;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketChatService {
    private final TicketChatMessageRepository messages;
    private final TicketRepository tickets;
    private final TicketService ticketService;
    private final AuditService audit;

    public TicketChatService(TicketChatMessageRepository messages, TicketRepository tickets,
                             TicketService ticketService, AuditService audit) {
        this.messages = messages;
        this.tickets = tickets;
        this.ticketService = ticketService;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<TicketChatMessageResponse> recent(Long ticketId, UserAccount actor) {
        ticketService.assertCanReadTicket(ticketId, actor);
        List<TicketChatMessage> recent = new ArrayList<>(messages.findTop100ByTicketIdOrderByCreatedAtDesc(ticketId));
        java.util.Collections.reverse(recent);
        return recent.stream().map(TicketChatService::toResponse).toList();
    }

    @Transactional
    public TicketChatMessageResponse send(Long ticketId, TicketChatSendRequest request, UserAccount actor) {
        ticketService.assertCanReadTicket(ticketId, actor);
        if (request == null || request.message() == null || request.message().isBlank()
                || request.message().length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a chat message of 1 to 4,000 characters");
        }
        if (!actor.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("tickets:comment"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot post in ticket chat");
        }
        Ticket ticket = tickets.findDetailedById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Closed tickets cannot receive chat messages");
        }
        TicketChatMessage saved = messages.save(new TicketChatMessage(ticket, actor, request.message().trim()));
        audit.record(actor, "TICKET_CHAT_MESSAGE", "TICKET", ticketId, "Posted a ticket chat message");
        return toResponse(saved);
    }

    private static TicketChatMessageResponse toResponse(TicketChatMessage message) {
        UserAccount sender = message.getSender();
        TicketUserSummary user = new TicketUserSummary(sender.getId(), sender.getFirstName(), sender.getLastName(),
                sender.getEmail(), sender.getDepartment() == null ? null : sender.getDepartment().getName());
        return new TicketChatMessageResponse(message.getId(), message.getMessage(), user, message.getCreatedAt());
    }
}
