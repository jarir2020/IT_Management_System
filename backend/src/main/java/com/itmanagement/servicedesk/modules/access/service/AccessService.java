package com.itmanagement.servicedesk.modules.access.service;

import com.itmanagement.servicedesk.modules.access.model.PermissionResponse;
import com.itmanagement.servicedesk.modules.access.model.RoleResponse;
import com.itmanagement.servicedesk.modules.access.repository.PermissionRepository;
import com.itmanagement.servicedesk.modules.access.repository.RoleRepository;
import com.itmanagement.servicedesk.modules.shared.ApiMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessService {
    private final RoleRepository roles;
    private final PermissionRepository permissions;

    public AccessService(RoleRepository roles, PermissionRepository permissions) {
        this.roles = roles;
        this.permissions = permissions;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roles.findAllByOrderByNameAsc().stream().map(ApiMapper::toRole).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> listPermissions() {
        return permissions.findAllByOrderByCodeAsc().stream().map(ApiMapper::toPermission).toList();
    }
}
