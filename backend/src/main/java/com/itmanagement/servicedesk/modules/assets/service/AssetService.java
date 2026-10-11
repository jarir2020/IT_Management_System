package com.itmanagement.servicedesk.modules.assets.service;

import com.itmanagement.servicedesk.modules.assets.model.Asset;
import com.itmanagement.servicedesk.modules.assets.model.AssetAssignment;
import com.itmanagement.servicedesk.modules.assets.model.AssetAssignmentResponse;
import com.itmanagement.servicedesk.modules.assets.model.AssetAssignmentRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetCategory;
import com.itmanagement.servicedesk.modules.assets.model.AssetDetailResponse;
import com.itmanagement.servicedesk.modules.assets.model.AssetDocument;
import com.itmanagement.servicedesk.modules.assets.model.AssetDocumentResponse;
import com.itmanagement.servicedesk.modules.assets.model.AssetDocumentType;
import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenance;
import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenanceRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenanceResponse;
import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenanceStatusRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetReturnRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetStatus;
import com.itmanagement.servicedesk.modules.assets.model.AssetSummary;
import com.itmanagement.servicedesk.modules.assets.model.AssetUserSummary;
import com.itmanagement.servicedesk.modules.assets.model.MaintenanceStatus;
import com.itmanagement.servicedesk.modules.assets.repository.AssetAssignmentRepository;
import com.itmanagement.servicedesk.modules.assets.repository.AssetDocumentRepository;
import com.itmanagement.servicedesk.modules.assets.repository.AssetMaintenanceRepository;
import com.itmanagement.servicedesk.modules.assets.repository.AssetRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.users.repository.UserAccountRepository;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssetService {
    private static final Set<MaintenanceStatus> ACTIVE_MAINTENANCE = Set.of(MaintenanceStatus.IN_PROGRESS);

    private final AssetRepository assets;
    private final AssetAssignmentRepository assignments;
    private final AssetMaintenanceRepository maintenance;
    private final AssetDocumentRepository documents;
    private final UserAccountRepository users;
    private final AssetFileStorage fileStorage;

    public AssetService(AssetRepository assets, AssetAssignmentRepository assignments,
                        AssetMaintenanceRepository maintenance, AssetDocumentRepository documents,
                        UserAccountRepository users, AssetFileStorage fileStorage) {
        this.assets = assets;
        this.assignments = assignments;
        this.maintenance = maintenance;
        this.documents = documents;
        this.users = users;
        this.fileStorage = fileStorage;
    }

    @Transactional(readOnly = true)
    public List<AssetSummary> listAssets(UserAccount actor) {
        if (hasAuthority(actor, "assets:read")) {
            return assets.findAllByOrderByUpdatedAtDesc().stream().map(this::toSummary).toList();
        }
        if (hasAuthority(actor, "assets:read_own")) {
            return assets.findAllByAssignedToIdOrderByUpdatedAtDesc(actor.getId()).stream()
                    .map(this::toSummary).toList();
        }
        throw forbidden("You cannot view the asset inventory");
    }

    @Transactional(readOnly = true)
    public List<AssetUserSummary> listAssignableUsers() {
        return users.findAllActiveForAssetAssignments().stream().map(AssetService::toUserSummary).toList();
    }

    @Transactional
    public AssetDetailResponse createAsset(AssetRequest request) {
        String tag = normalizeTag(request.assetTag());
        ensureUniqueAssetDetails(tag, clean(request.serialNumber()), null);
        Asset asset = new Asset(tag, request.name().trim(), request.category(), clean(request.description()),
                clean(request.manufacturer()), clean(request.modelNumber()), clean(request.serialNumber()),
                request.purchaseDate(), request.purchaseCost(), request.warrantyExpiresOn());
        return toDetail(assets.save(asset));
    }

    @Transactional(readOnly = true)
    public AssetDetailResponse getAsset(Long assetId, UserAccount actor) {
        return toDetail(findReadableAsset(assetId, actor));
    }

    @Transactional
    public AssetDetailResponse updateAsset(Long assetId, AssetRequest request) {
        Asset asset = findLockedAsset(assetId);
        if (asset.getStatus() == AssetStatus.RETIRED) {
            throw conflict("Retired assets cannot be edited");
        }
        String tag = normalizeTag(request.assetTag());
        ensureUniqueAssetDetails(tag, clean(request.serialNumber()), assetId);
        asset.updateDetails(tag, request.name().trim(), request.category(), clean(request.description()),
                clean(request.manufacturer()), clean(request.modelNumber()), clean(request.serialNumber()),
                request.purchaseDate(), request.purchaseCost(), request.warrantyExpiresOn());
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse assignAsset(Long assetId, AssetAssignmentRequest request, UserAccount actor) {
        Asset asset = findLockedAsset(assetId);
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw conflict("Only available assets can be assigned");
        }
        UserAccount assignee = users.findDetailedById(request.userId())
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> badRequest("Choose an active user account"));
        asset.assignTo(assignee);
        assignments.save(new AssetAssignment(asset, assignee, actor, clean(request.note())));
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse assignAssetForRequest(Long assetId, Long userId, Long requestId,
                                                     AssetCategory expectedCategory, String note,
                                                     UserAccount actor) {
        Asset asset = findLockedAsset(assetId);
        if (asset.getStatus() != AssetStatus.AVAILABLE) {
            throw conflict("Only available assets can be allocated to a request");
        }
        if (asset.getCategory() != expectedCategory) {
            throw conflict("Choose an available asset in the requested category");
        }
        UserAccount assignee = users.findDetailedById(userId)
                .filter(UserAccount::isEnabled)
                .orElseThrow(() -> badRequest("The requester's account is no longer active"));
        asset.assignTo(assignee);
        assignments.save(new AssetAssignment(asset, assignee, actor, clean(note), requestId));
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse returnAsset(Long assetId, Long assignmentId,
                                           AssetReturnRequest request, UserAccount actor) {
        Asset asset = findLockedAsset(assetId);
        AssetAssignment assignment = assignments.findActiveByAssetId(assetId)
                .filter(current -> current.getId().equals(assignmentId))
                .orElseThrow(() -> conflict("This assignment is no longer active"));
        assignment.markReturned(actor, clean(request.conditionNotes()));
        asset.returnToInventory(request.needsMaintenance());
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse startMaintenance(Long assetId, AssetMaintenanceRequest request, UserAccount actor) {
        Asset asset = findLockedAsset(assetId);
        if (asset.getStatus() == AssetStatus.RETIRED || asset.getStatus() == AssetStatus.ASSIGNED) {
            throw conflict("Return the asset before starting maintenance");
        }
        if (maintenance.existsByAssetIdAndStatusIn(assetId, ACTIVE_MAINTENANCE)) {
            throw conflict("This asset already has active maintenance");
        }
        asset.startMaintenance();
        maintenance.save(new AssetMaintenance(asset, actor, request.summary().trim(),
                clean(request.vendor()), clean(request.notes()), request.cost(), request.startedOn()));
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse changeMaintenanceStatus(Long assetId, Long maintenanceId,
                                                        AssetMaintenanceStatusRequest request) {
        Asset asset = findLockedAsset(assetId);
        AssetMaintenance record = maintenance.findByIdAndAssetId(maintenanceId, assetId)
                .orElseThrow(() -> notFound("Maintenance record not found"));
        if (record.getStatus() != MaintenanceStatus.IN_PROGRESS
                || (request.status() != MaintenanceStatus.COMPLETED
                && request.status() != MaintenanceStatus.CANCELLED)) {
            throw conflict("Active maintenance can only be completed or cancelled");
        }
        record.changeStatus(request.status());
        if (asset.getStatus() == AssetStatus.UNDER_MAINTENANCE) {
            asset.finishMaintenance();
        }
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse addDocument(Long assetId, AssetDocumentType type,
                                           MultipartFile file, UserAccount actor) {
        Asset asset = findReadableAsset(assetId, actor);
        AssetFileStorage.StoredFile stored = fileStorage.store(file);
        removeStoredFileIfTransactionRollsBack(stored.storageKey());
        documents.save(new AssetDocument(asset, actor, type, stored.originalFilename(),
                stored.storageKey(), stored.sizeBytes()));
        return toDetail(asset);
    }

    @Transactional(readOnly = true)
    public AssetDownload downloadDocument(Long assetId, Long documentId, UserAccount actor) {
        Asset asset = findReadableAsset(assetId, actor);
        AssetDocument document = documents.findByIdAndAssetId(documentId, asset.getId())
                .orElseThrow(() -> notFound("Asset document not found"));
        Path path = fileStorage.pathFor(document.getStorageKey());
        return new AssetDownload(path, document.getOriginalFilename(), document.getSizeBytes());
    }

    @Transactional
    public AssetDetailResponse deleteDocument(Long assetId, Long documentId, UserAccount actor) {
        Asset asset = findReadableAsset(assetId, actor);
        AssetDocument document = documents.findByIdAndAssetId(documentId, asset.getId())
                .orElseThrow(() -> notFound("Asset document not found"));
        String storageKey = document.getStorageKey();
        documents.delete(document);
        deleteStoredFileAfterCommit(storageKey);
        return toDetail(asset);
    }

    @Transactional
    public AssetDetailResponse retireAsset(Long assetId) {
        Asset asset = findLockedAsset(assetId);
        if (asset.getStatus() == AssetStatus.ASSIGNED
                || (asset.getStatus() == AssetStatus.UNDER_MAINTENANCE
                && maintenance.existsByAssetIdAndStatusIn(assetId, ACTIVE_MAINTENANCE))) {
            throw conflict("Return the asset and finish active maintenance before retiring it");
        }
        if (asset.getStatus() == AssetStatus.RETIRED) return toDetail(asset);
        asset.retire();
        return toDetail(asset);
    }

    private Asset findReadableAsset(Long assetId, UserAccount actor) {
        Asset asset = assets.findDetailedById(assetId).orElseThrow(() -> notFound("Asset not found"));
        boolean canReadAll = hasAuthority(actor, "assets:read");
        boolean assignedToActor = asset.getAssignedTo() != null
                && asset.getAssignedTo().getId().equals(actor.getId());
        if (!canReadAll && (!hasAuthority(actor, "assets:read_own") || !assignedToActor)) {
            throw notFound("Asset not found");
        }
        return asset;
    }

    private Asset findLockedAsset(Long assetId) {
        return assets.findByIdForUpdate(assetId).orElseThrow(() -> notFound("Asset not found"));
    }

    private void ensureUniqueAssetDetails(String tag, String serialNumber, Long exceptId) {
        boolean duplicateTag = exceptId == null
                ? assets.existsByAssetTagIgnoreCase(tag)
                : assets.existsByAssetTagIgnoreCaseAndIdNot(tag, exceptId);
        if (duplicateTag) throw conflict("An asset with that tag already exists");
        if (serialNumber != null) {
            boolean duplicateSerial = exceptId == null
                    ? assets.existsBySerialNumberIgnoreCase(serialNumber)
                    : assets.existsBySerialNumberIgnoreCaseAndIdNot(serialNumber, exceptId);
            if (duplicateSerial) throw conflict("An asset with that serial number already exists");
        }
    }

    private AssetDetailResponse toDetail(Asset asset) {
        List<AssetAssignmentResponse> assignmentHistory = assignments
                .findAllByAssetIdOrderByAssignedAtDesc(asset.getId()).stream()
                .map(item -> new AssetAssignmentResponse(item.getId(), toUserSummary(item.getAssignedTo()),
                        toUserSummary(item.getAssignedBy()), toUserSummary(item.getReturnedBy()),
                        item.getAssignmentNote(), item.getReturnCondition(), item.getAssignedAt(), item.getReturnedAt(),
                        item.getEquipmentRequestId()))
                .toList();
        List<AssetMaintenanceResponse> maintenanceHistory = maintenance
                .findAllByAssetIdOrderByCreatedAtDesc(asset.getId()).stream()
                .map(item -> new AssetMaintenanceResponse(item.getId(), item.getSummary(), item.getVendor(),
                        item.getNotes(), item.getCost(), item.getStartedOn(), item.getCompletedOn(),
                        item.getStatus(), toUserSummary(item.getCreatedBy()), item.getCreatedAt()))
                .toList();
        List<AssetDocumentResponse> assetDocuments = documents
                .findAllByAssetIdOrderByUploadedAtDesc(asset.getId()).stream()
                .map(item -> new AssetDocumentResponse(item.getId(), item.getType(), item.getOriginalFilename(),
                        item.getSizeBytes(), toUserSummary(item.getUploadedBy()), item.getUploadedAt()))
                .toList();
        return new AssetDetailResponse(asset.getId(), asset.getAssetTag(), asset.getName(), asset.getCategory(),
                asset.getDescription(), asset.getManufacturer(), asset.getModelNumber(), asset.getSerialNumber(),
                asset.getPurchaseDate(), asset.getPurchaseCost(), asset.getWarrantyExpiresOn(), asset.getStatus(),
                toUserSummary(asset.getAssignedTo()), asset.getCreatedAt(), asset.getUpdatedAt(),
                assignmentHistory, maintenanceHistory, assetDocuments);
    }

    private AssetSummary toSummary(Asset asset) {
        return new AssetSummary(asset.getId(), asset.getAssetTag(), asset.getName(), asset.getCategory(),
                asset.getStatus(), toUserSummary(asset.getAssignedTo()), asset.getWarrantyExpiresOn(), asset.getUpdatedAt());
    }

    private static AssetUserSummary toUserSummary(UserAccount user) {
        if (user == null) return null;
        return new AssetUserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }

    private void removeStoredFileIfTransactionRollsBack(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) fileStorage.deleteQuietly(storageKey);
            }
        });
    }

    private void deleteStoredFileAfterCommit(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            fileStorage.deleteQuietly(storageKey);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { fileStorage.deleteQuietly(storageKey); }
        });
    }

    private static boolean hasAuthority(UserAccount actor, String authority) {
        return actor.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    private static String normalizeTag(String tag) { return tag.trim().toUpperCase(Locale.ROOT); }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
