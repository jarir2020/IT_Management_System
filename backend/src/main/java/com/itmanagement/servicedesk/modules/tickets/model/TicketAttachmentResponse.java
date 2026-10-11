package com.itmanagement.servicedesk.modules.tickets.model;

import java.time.Instant;

public record TicketAttachmentResponse(
        Long id,
        String originalFilename,
        long sizeBytes,
        TicketUserSummary uploadedBy,
        Instant uploadedAt) {
}
