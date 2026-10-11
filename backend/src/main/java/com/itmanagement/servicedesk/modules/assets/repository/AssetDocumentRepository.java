package com.itmanagement.servicedesk.modules.assets.repository;

import com.itmanagement.servicedesk.modules.assets.model.AssetDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetDocumentRepository extends JpaRepository<AssetDocument, Long> {
    @EntityGraph(attributePaths = {"uploadedBy", "uploadedBy.department"})
    List<AssetDocument> findAllByAssetIdOrderByUploadedAtDesc(Long assetId);

    @EntityGraph(attributePaths = {"uploadedBy", "asset"})
    Optional<AssetDocument> findByIdAndAssetId(Long id, Long assetId);
}
