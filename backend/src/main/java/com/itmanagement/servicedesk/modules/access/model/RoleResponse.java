package com.itmanagement.servicedesk.modules.access.model;

import java.util.List;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean systemRole,
        List<PermissionResponse> permissions) {
}
