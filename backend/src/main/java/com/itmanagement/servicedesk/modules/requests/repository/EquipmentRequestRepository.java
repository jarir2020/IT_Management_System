package com.itmanagement.servicedesk.modules.requests.repository;

import com.itmanagement.servicedesk.modules.requests.model.EquipmentRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface EquipmentRequestRepository extends JpaRepository<EquipmentRequest, Long> {
    @EntityGraph(attributePaths = {"requester", "requester.department", "department", "reviewedBy",
            "reviewedBy.department", "procurementUpdatedBy", "procurementUpdatedBy.department"})
    @Query("select r from EquipmentRequest r order by r.updatedAt desc")
    List<EquipmentRequest> findAllDetailed();

    @EntityGraph(attributePaths = {"requester", "requester.department", "department", "reviewedBy",
            "reviewedBy.department", "procurementUpdatedBy", "procurementUpdatedBy.department"})
    @Query("select r from EquipmentRequest r where r.requester.id = :requesterId order by r.updatedAt desc")
    List<EquipmentRequest> findAllForRequester(@Param("requesterId") Long requesterId);

    @EntityGraph(attributePaths = {"requester", "requester.department", "department", "reviewedBy",
            "reviewedBy.department", "procurementUpdatedBy", "procurementUpdatedBy.department"})
    @Query("select r from EquipmentRequest r where r.department.id = :departmentId order by r.updatedAt desc")
    List<EquipmentRequest> findAllForDepartment(@Param("departmentId") Long departmentId);

    @EntityGraph(attributePaths = {"requester", "requester.department", "department", "reviewedBy",
            "reviewedBy.department", "procurementUpdatedBy", "procurementUpdatedBy.department"})
    @Query("select r from EquipmentRequest r where r.id = :id")
    Optional<EquipmentRequest> findDetailedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from EquipmentRequest r where r.id = :id")
    Optional<EquipmentRequest> findByIdForUpdate(@Param("id") Long id);
}
