package com.itmanagement.servicedesk.modules.tickets.model;

import java.time.Instant;

public record TicketSlaSummary(long responseTargetMinutes,
                               long resolutionTargetMinutes,
                               Instant firstResponseDueAt,
                               Instant resolutionDueAt,
                               Instant firstRespondedAt,
                               boolean firstResponseBreached,
                               boolean resolutionBreached) {
}
