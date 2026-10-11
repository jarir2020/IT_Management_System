package com.itmanagement.servicedesk.modules.users.repository;

import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    Optional<UserAccount> findByEmailIgnoreCase(String email);

    @Query("select distinct u from UserAccount u order by u.lastName, u.firstName")
    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    List<UserAccount> findAllDetailed();

    @Query("select distinct u from UserAccount u where u.id = :id")
    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    Optional<UserAccount> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"department", "roles", "roles.permissions"})
    @Query("select distinct u from UserAccount u join u.roles r where u.enabled = true and r.code in ('SUPPORT_AGENT', 'ADMIN') order by u.lastName, u.firstName")
    List<UserAccount> findActiveSupportAgents();

    long countByDepartmentId(Long departmentId);

    @Query("select count(distinct u) from UserAccount u join u.roles r where r.code = :roleCode and u.enabled = true")
    long countEnabledUsersWithRole(@Param("roleCode") String roleCode);
}
