package com.itmanagement.servicedesk.modules.shared;

import com.itmanagement.servicedesk.modules.departments.model.DepartmentResponse;
import com.itmanagement.servicedesk.modules.departments.model.DepartmentSummary;
import com.itmanagement.servicedesk.modules.access.model.PermissionResponse;
import com.itmanagement.servicedesk.modules.access.model.RoleResponse;
import com.itmanagement.servicedesk.modules.access.model.RoleSummary;
import com.itmanagement.servicedesk.modules.users.model.UserResponse;
import com.itmanagement.servicedesk.modules.departments.model.Department;
import com.itmanagement.servicedesk.modules.access.model.Permission;
import com.itmanagement.servicedesk.modules.access.model.Role;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApiMapper {
    private ApiMapper() {
    }

    public static DepartmentResponse toDepartment(Department department) {
        return new DepartmentResponse(
                department.getId(), department.getName(), department.getCode(),
                department.getDescription(), department.isActive(),
                department.getCreatedAt(), department.getUpdatedAt());
    }

    public static DepartmentSummary toDepartmentSummary(Department department) {
        return department == null ? null : new DepartmentSummary(
                department.getId(), department.getCode(), department.getName());
    }

    public static PermissionResponse toPermission(Permission permission) {
        return new PermissionResponse(permission.getCode(), permission.getDescription());
    }

    public static RoleResponse toRole(Role role) {
        return new RoleResponse(role.getId(), role.getCode(), role.getName(),
                role.getDescription(), role.isSystemRole(), role.getPermissions().stream()
                        .map(ApiMapper::toPermission)
                        .sorted((left, right) -> left.code().compareTo(right.code()))
                        .toList());
    }

    public static RoleSummary toRoleSummary(Role role) {
        return new RoleSummary(role.getId(), role.getCode(), role.getName());
    }

    public static UserResponse toUser(UserAccount user) {
        Set<RoleSummary> roles = user.getRoles().stream()
                .map(ApiMapper::toRoleSummary)
                .sorted((left, right) -> left.name().compareTo(right.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new UserResponse(user.getId(), user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getEnabled(), toDepartmentSummary(user.getDepartment()),
                roles, permissions, user.getCreatedAt(), user.getUpdatedAt());
    }
}
