package com.itmanagement.servicedesk.modules.tickets.repository;

import com.itmanagement.servicedesk.modules.tickets.model.TicketComment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    @EntityGraph(attributePaths = {"author", "author.department"})
    List<TicketComment> findAllByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
