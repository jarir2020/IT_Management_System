package com.itmanagement.servicedesk.modules.access.repository;

import com.itmanagement.servicedesk.modules.access.model.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    @EntityGraph(attributePaths = "permissions")
    List<Role> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findByCode(String code);
}
