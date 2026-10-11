package com.itmanagement.servicedesk.modules.assets.repository;

import com.itmanagement.servicedesk.modules.assets.model.AssetAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {
    @EntityGraph(attributePaths = {"assignedTo", "assignedTo.department", "assignedBy", "assignedBy.department",
            "returnedBy", "returnedBy.department"})
    List<AssetAssignment> findAllByAssetIdOrderByAssignedAtDesc(Long assetId);

    @EntityGraph(attributePaths = {"assignedTo", "assignedBy", "asset"})
    @Query("select a from AssetAssignment a where a.asset.id = :assetId and a.returnedAt is null")
    Optional<AssetAssignment> findActiveByAssetId(@Param("assetId") Long assetId);

    Optional<AssetAssignment> findByIdAndAssetId(Long id, Long assetId);
}
