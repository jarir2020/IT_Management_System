package com.itmanagement.servicedesk.modules.assets.repository;

import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenance;
import com.itmanagement.servicedesk.modules.assets.model.MaintenanceStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetMaintenanceRepository extends JpaRepository<AssetMaintenance, Long> {
    @EntityGraph(attributePaths = {"createdBy", "createdBy.department"})
    List<AssetMaintenance> findAllByAssetIdOrderByCreatedAtDesc(Long assetId);

    Optional<AssetMaintenance> findByIdAndAssetId(Long id, Long assetId);

    boolean existsByAssetIdAndStatusIn(Long assetId, Collection<MaintenanceStatus> statuses);

    @Query("select m.asset.id as assetId, coalesce(sum(m.cost), 0) as totalCost "
            + "from AssetMaintenance m where m.asset.id in :assetIds group by m.asset.id")
    List<AssetMaintenanceCostProjection> sumCostsByAssetIds(@Param("assetIds") Collection<Long> assetIds);
}
