package com.itmanagement.servicedesk.modules.departments.controller;

import com.itmanagement.servicedesk.modules.departments.model.DepartmentRequest;
import com.itmanagement.servicedesk.modules.departments.model.DepartmentResponse;
import com.itmanagement.servicedesk.modules.departments.service.DepartmentService;
import com.itmanagement.servicedesk.modules.departments.routes.DepartmentRoutes;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(DepartmentRoutes.BASE_PATH)
public class DepartmentController {
    private final DepartmentService departments;

    public DepartmentController(DepartmentService departments) {
        this.departments = departments;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('departments:read')")
    public List<DepartmentResponse> list() {
        return departments.list();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('departments:write')")
    public DepartmentResponse create(@Valid @RequestBody DepartmentRequest request) {
        return departments.create(request);
    }

    @PutMapping(DepartmentRoutes.BY_ID)
    @PreAuthorize("hasAuthority('departments:write')")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return departments.update(id, request);
    }

    @DeleteMapping(DepartmentRoutes.BY_ID)
    @PreAuthorize("hasAuthority('departments:write')")
    public DepartmentResponse deactivate(@PathVariable Long id) {
        return departments.deactivate(id);
    }
}
