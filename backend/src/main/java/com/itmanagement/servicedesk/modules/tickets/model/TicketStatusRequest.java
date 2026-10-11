package com.itmanagement.servicedesk.modules.tickets.model;

import jakarta.validation.constraints.NotNull;

public record TicketStatusRequest(@NotNull TicketStatus status) {
}
