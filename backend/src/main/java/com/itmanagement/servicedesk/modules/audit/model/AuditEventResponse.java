package com.itmanagement.servicedesk.modules.audit.model;

import java.time.Instant;

public record AuditEventResponse(Long id, Long actorId, String actorEmail, String action,
                                String entityType, Long entityId, String summary, Instant createdAt) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getActorId(), event.getActorEmail(),
                event.getAction(), event.getEntityType(), event.getEntityId(), event.getSummary(),
                event.getCreatedAt());
    }
}
