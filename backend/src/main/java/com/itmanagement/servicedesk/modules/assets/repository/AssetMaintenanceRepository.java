package com.itmanagement.servicedesk.modules.assets.repository;

import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenance;
import com.itmanagement.servicedesk.modules.assets.model.MaintenanceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, Long> {
    @EntityGraph(attributePaths = {"createdBy", "createdBy.department"})
    List<AssetMaintenance> findAllByAssetIdOrderByCreatedAtDesc(Long assetId);

    Optional<AssetMaintenance> findByIdAndAssetId(Long id, Long assetId);

    boolean existsByAssetIdAndStatusIn(Long assetId, Collection<MaintenanceStatus> statuses);
}
