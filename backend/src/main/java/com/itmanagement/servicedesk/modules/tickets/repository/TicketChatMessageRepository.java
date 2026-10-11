package com.itmanagement.servicedesk.modules.tickets.repository;

import com.itmanagement.servicedesk.modules.tickets.model.TicketChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketChatMessageRepository extends JpaRepository<TicketChatMessage, Long> {
    @EntityGraph(attributePaths = {"sender", "sender.department"})
    List<TicketChatMessage> findTop100ByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
