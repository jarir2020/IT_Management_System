package com.itmanagement.servicedesk.modules.tickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketCommentRequest(
        @NotBlank @Size(max = 10000) String message,
        boolean internalNote) {
}
