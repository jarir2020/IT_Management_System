package com.itmanagement.servicedesk.modules.access.controller;

import com.itmanagement.servicedesk.modules.access.model.PermissionResponse;
import com.itmanagement.servicedesk.modules.access.model.RoleResponse;
import com.itmanagement.servicedesk.modules.access.routes.AccessRoutes;
import com.itmanagement.servicedesk.modules.access.service.AccessService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AccessRoutes.API_BASE)
public class AccessController {
    private final AccessService access;

    public AccessController(AccessService access) {
        this.access = access;
    }

    @GetMapping(AccessRoutes.ROLES)
    @PreAuthorize("hasAuthority('roles:read')")
    public List<RoleResponse> listRoles() {
        return access.listRoles();
    }

    @GetMapping(AccessRoutes.PERMISSIONS)
    @PreAuthorize("hasAuthority('roles:read')")
    public List<PermissionResponse> listPermissions() {
        return access.listPermissions();
    }
}
