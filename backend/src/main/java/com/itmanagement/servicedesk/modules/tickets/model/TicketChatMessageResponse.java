package com.itmanagement.servicedesk.modules.tickets.model;

import java.time.Instant;

public record TicketChatMessageResponse(Long id, String message, TicketUserSummary sender, Instant createdAt) {
}
