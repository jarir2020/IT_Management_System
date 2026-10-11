package com.itmanagement.servicedesk.modules.audit.controller;

import com.itmanagement.servicedesk.modules.audit.model.AuditEventResponse;
import com.itmanagement.servicedesk.modules.audit.routes.AuditRoutes;
import com.itmanagement.servicedesk.modules.audit.service.AuditService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AuditRoutes.BASE_PATH)
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('audit:read')")
    public List<AuditEventResponse> recent(@RequestParam(defaultValue = "200") int limit) {
        return audit.recent(limit);
    }
}
