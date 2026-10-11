package com.itmanagement.servicedesk.modules.assets.repository;

import com.itmanagement.servicedesk.modules.assets.model.Asset;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    @EntityGraph(attributePaths = {"assignedTo", "assignedTo.department"})
    List<Asset> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = {"assignedTo", "assignedTo.department"})
    List<Asset> findAllByAssignedToIdOrderByUpdatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"assignedTo", "assignedTo.department"})
    @Query("select a from Asset a where a.id = :assetId")
    Optional<Asset> findDetailedById(@Param("assetId") Long assetId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.id = :assetId")
    Optional<Asset> findByIdForUpdate(@Param("assetId") Long assetId);

    boolean existsByAssetTagIgnoreCase(String assetTag);
    boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id);
    boolean existsBySerialNumberIgnoreCase(String serialNumber);
    boolean existsBySerialNumberIgnoreCaseAndIdNot(String serialNumber, Long id);
}
