package com.itmanagement.servicedesk.modules.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import com.itmanagement.servicedesk.modules.tickets.model.TicketPriority;
import com.itmanagement.servicedesk.modules.tickets.model.TicketSlaSummary;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

class TicketSlaCalculatorTest {
    private final TicketSlaCalculator calculator = new TicketSlaCalculator();

    @Test
    void normalPriorityTargetsAndExactDeadlinesAreNotBreached() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Ticket ticket = ticket(TicketPriority.NORMAL, createdAt, null, null);

        TicketSlaSummary summary = calculator.summarize(ticket, createdAt.plus(8, ChronoUnit.HOURS));

        assertThat(summary.responseTargetMinutes()).isEqualTo(480);
        assertThat(summary.resolutionTargetMinutes()).isEqualTo(4_320);
        assertThat(summary.firstResponseDueAt()).isEqualTo(createdAt.plus(8, ChronoUnit.HOURS));
        assertThat(summary.firstResponseBreached()).isFalse();
        assertThat(summary.resolutionBreached()).isFalse();
    }

    @Test
    void responseAndResolutionBreachAfterTheirTargets() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Ticket ticket = ticket(TicketPriority.URGENT, createdAt, null, null);

        TicketSlaSummary summary = calculator.summarize(ticket, createdAt.plus(241, ChronoUnit.MINUTES));

        assertThat(summary.firstResponseBreached()).isTrue();
        assertThat(summary.resolutionBreached()).isTrue();
    }

    @Test
    void actualResponsesAreComparedToTheResponseTarget() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Ticket onTime = ticket(TicketPriority.HIGH, createdAt, createdAt.plus(4, ChronoUnit.HOURS), null);
        Ticket late = ticket(TicketPriority.HIGH, createdAt, createdAt.plus(4, ChronoUnit.HOURS).plusSeconds(1), null);

        assertThat(calculator.summarize(onTime, createdAt.plus(1, ChronoUnit.DAYS)).firstResponseBreached()).isFalse();
        assertThat(calculator.summarize(late, createdAt.plus(1, ChronoUnit.DAYS)).firstResponseBreached()).isTrue();
    }

    private static Ticket ticket(TicketPriority priority, Instant createdAt,
                                 Instant firstResponseAt, Instant resolvedAt) {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getPriority()).thenReturn(priority);
        when(ticket.getCreatedAt()).thenReturn(createdAt);
        when(ticket.getFirstRespondedAt()).thenReturn(firstResponseAt);
        when(ticket.getResolvedAt()).thenReturn(resolvedAt);
        return ticket;
    }
}
