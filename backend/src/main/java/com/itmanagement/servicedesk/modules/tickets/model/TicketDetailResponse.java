package com.itmanagement.servicedesk.modules.tickets.model;

import java.time.Instant;
import java.util.List;

public record TicketDetailResponse(
        Long id,
        String title,
        String description,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        TicketUserSummary requester,
        TicketUserSummary assignee,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        Instant closedAt,
        List<TicketCommentResponse> comments,
        List<TicketAttachmentResponse> attachments) {
}
