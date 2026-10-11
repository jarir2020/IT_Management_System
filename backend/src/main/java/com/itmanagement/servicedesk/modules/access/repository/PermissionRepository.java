package com.itmanagement.servicedesk.modules.access.repository;

import com.itmanagement.servicedesk.modules.access.model.Permission;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    List<Permission> findAllByOrderByCodeAsc();
}
