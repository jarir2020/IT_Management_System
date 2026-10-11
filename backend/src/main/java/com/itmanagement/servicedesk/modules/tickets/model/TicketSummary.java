package com.itmanagement.servicedesk.modules.tickets.model;

import java.time.Instant;

public record TicketSummary(
        Long id,
        String title,
        TicketCategory category,
        TicketPriority priority,
        TicketStatus status,
        TicketUserSummary requester,
        TicketUserSummary assignee,
        Instant createdAt,
        Instant updatedAt,
        TicketSlaSummary sla) {
}
