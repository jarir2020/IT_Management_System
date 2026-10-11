package com.itmanagement.servicedesk.modules.tickets.model;

public record TicketUserSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        String departmentName) {
}
