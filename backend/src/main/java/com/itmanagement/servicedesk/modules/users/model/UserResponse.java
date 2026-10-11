package com.itmanagement.servicedesk.modules.users.model;

import com.itmanagement.servicedesk.modules.access.model.RoleSummary;
import com.itmanagement.servicedesk.modules.departments.model.DepartmentSummary;
import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        DepartmentSummary department,
        Set<RoleSummary> roles,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt) {
    public String displayName() {
        return firstName + " " + lastName;
    }
}
