package com.itmanagement.servicedesk.modules.tickets.service;

import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import com.itmanagement.servicedesk.modules.tickets.model.TicketPriority;
import com.itmanagement.servicedesk.modules.tickets.model.TicketSlaSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sla")
public class TicketSlaCalculator {
    private Map<TicketPriority, Targets> targets = defaults();

    public TicketSlaSummary summarize(Ticket ticket) {
        return summarize(ticket, Instant.now());
    }

    public TicketSlaSummary summarize(Ticket ticket, Instant now) {
        Targets target = targets.getOrDefault(ticket.getPriority(), defaults().get(TicketPriority.NORMAL));
        Instant responseDueAt = ticket.getCreatedAt().plus(Duration.ofMinutes(target.getFirstResponseMinutes()));
        Instant resolutionDueAt = ticket.getCreatedAt().plus(Duration.ofMinutes(target.getResolutionMinutes()));
        boolean responseBreached = ticket.getFirstRespondedAt() == null
                ? now.isAfter(responseDueAt)
                : ticket.getFirstRespondedAt().isAfter(responseDueAt);
        boolean resolutionBreached = ticket.getResolvedAt() == null
                ? now.isAfter(resolutionDueAt)
                : ticket.getResolvedAt().isAfter(resolutionDueAt);
        return new TicketSlaSummary(target.getFirstResponseMinutes(), target.getResolutionMinutes(),
                responseDueAt, resolutionDueAt, ticket.getFirstRespondedAt(), responseBreached,
                resolutionBreached);
    }

    public Map<TicketPriority, Targets> getTargets() { return targets; }
    public void setTargets(Map<TicketPriority, Targets> targets) {
        if (targets != null && !targets.isEmpty()) {
            Map<TicketPriority, Targets> merged = defaults();
            merged.putAll(targets);
            this.targets = merged;
        }
    }

    private static Map<TicketPriority, Targets> defaults() {
        Map<TicketPriority, Targets> defaults = new EnumMap<>(TicketPriority.class);
        defaults.put(TicketPriority.URGENT, new Targets(60, 240));
        defaults.put(TicketPriority.HIGH, new Targets(240, 1_440));
        defaults.put(TicketPriority.NORMAL, new Targets(480, 4_320));
        defaults.put(TicketPriority.LOW, new Targets(1_440, 7_200));
        return defaults;
    }

    public static class Targets {
        private long firstResponseMinutes;
        private long resolutionMinutes;

        public Targets() {
        }

        public Targets(long firstResponseMinutes, long resolutionMinutes) {
            this.firstResponseMinutes = firstResponseMinutes;
            this.resolutionMinutes = resolutionMinutes;
        }

        public long getFirstResponseMinutes() { return firstResponseMinutes; }
        public void setFirstResponseMinutes(long firstResponseMinutes) { this.firstResponseMinutes = firstResponseMinutes; }
        public long getResolutionMinutes() { return resolutionMinutes; }
        public void setResolutionMinutes(long resolutionMinutes) { this.resolutionMinutes = resolutionMinutes; }
    }
}
