package com.itmanagement.servicedesk.modules.tickets.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketChatSendRequest(@NotBlank @Size(max = 4000) String message) {
}
