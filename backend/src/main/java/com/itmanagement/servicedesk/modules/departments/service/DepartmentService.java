package com.itmanagement.servicedesk.modules.departments.service;

import com.itmanagement.servicedesk.modules.shared.ApiMapper;
import com.itmanagement.servicedesk.modules.departments.model.DepartmentRequest;
import com.itmanagement.servicedesk.modules.departments.model.DepartmentResponse;
import com.itmanagement.servicedesk.modules.departments.model.Department;
import com.itmanagement.servicedesk.modules.departments.repository.DepartmentRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DepartmentService {
    private final DepartmentRepository departments;

    public DepartmentService(DepartmentRepository departments) {
        this.departments = departments;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        return departments.findAllByOrderByNameAsc().stream()
                .map(ApiMapper::toDepartment)
                .toList();
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        String code = normalizeCode(request.code());
        if (departments.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A department with that name already exists");
        }
        if (departments.existsByCodeIgnoreCase(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A department with that code already exists");
        }
        Department department = new Department(name, code, clean(request.description()), request.active());
        return ApiMapper.toDepartment(departments.save(department));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = getDepartment(id);
        String name = request.name().trim();
        String code = normalizeCode(request.code());
        if (departments.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A department with that name already exists");
        }
        if (departments.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A department with that code already exists");
        }
        department.setName(name);
        department.setCode(code);
        department.setDescription(clean(request.description()));
        department.setActive(request.active());
        return ApiMapper.toDepartment(department);
    }

    @Transactional
    public DepartmentResponse deactivate(Long id) {
        Department department = getDepartment(id);
        department.setActive(false);
        return ApiMapper.toDepartment(department);
    }

    private Department getDepartment(Long id) {
        return departments.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String clean(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
