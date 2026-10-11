package com.itmanagement.servicedesk.modules.departments.model;

import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}
