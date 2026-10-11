package com.itmanagement.servicedesk.modules.tickets.repository;

import com.itmanagement.servicedesk.modules.tickets.model.TicketAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {
    @EntityGraph(attributePaths = {"uploadedBy", "uploadedBy.department"})
    List<TicketAttachment> findAllByTicketIdOrderByUploadedAtAsc(Long ticketId);

    @EntityGraph(attributePaths = {"uploadedBy", "uploadedBy.department", "ticket"})
    Optional<TicketAttachment> findByIdAndTicketId(Long id, Long ticketId);
}
