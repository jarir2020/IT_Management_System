package com.itmanagement.servicedesk.modules.tickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketUpdateRequest(
        @NotBlank @Size(max = 160) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotNull TicketCategory category,
        @NotNull TicketPriority priority) {
}
