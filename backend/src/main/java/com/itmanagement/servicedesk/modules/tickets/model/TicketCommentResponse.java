package com.itmanagement.servicedesk.modules.tickets.model;

import java.time.Instant;

public record TicketCommentResponse(
        Long id,
        String message,
        boolean internalNote,
        TicketUserSummary author,
        Instant createdAt) {
}
