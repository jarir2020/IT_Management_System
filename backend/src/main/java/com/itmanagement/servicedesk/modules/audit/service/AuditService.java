package com.itmanagement.servicedesk.modules.audit.service;

import com.itmanagement.servicedesk.modules.audit.model.AuditEvent;
import com.itmanagement.servicedesk.modules.audit.model.AuditEventResponse;
import com.itmanagement.servicedesk.modules.audit.repository.AuditEventRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UserAccount actor, String action, String entityType, Long entityId, String summary) {
        String safeSummary = summary == null || summary.isBlank() ? action : summary.trim();
        if (safeSummary.length() > 500) safeSummary = safeSummary.substring(0, 500);
        events.save(new AuditEvent(actor, action, entityType, entityId, safeSummary));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recent(int limit) {
        int safeLimit = Math.clamp(limit, 1, 500);
        return events.findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit)).stream()
                .map(AuditEventResponse::from).toList();
    }
}
