package com.itmanagement.servicedesk.modules.audit.repository;

import com.itmanagement.servicedesk.modules.audit.model.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
