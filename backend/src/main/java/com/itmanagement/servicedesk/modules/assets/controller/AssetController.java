package com.itmanagement.servicedesk.modules.assets.controller;

import com.itmanagement.servicedesk.modules.assets.model.AssetAssignmentRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetDetailResponse;
import com.itmanagement.servicedesk.modules.assets.model.AssetDocumentType;
import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenanceRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetMaintenanceStatusRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetReturnRequest;
import com.itmanagement.servicedesk.modules.assets.model.AssetSummary;
import com.itmanagement.servicedesk.modules.assets.model.AssetUserSummary;
import com.itmanagement.servicedesk.modules.assets.routes.AssetRoutes;
import com.itmanagement.servicedesk.modules.assets.service.AssetDownload;
import com.itmanagement.servicedesk.modules.assets.service.AssetService;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(AssetRoutes.BASE_PATH)
public class AssetController {
    private final AssetService assets;

    public AssetController(AssetService assets) {
        this.assets = assets;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assets:read') or hasAuthority('assets:read_own')")
    public List<AssetSummary> list(@AuthenticationPrincipal UserAccount actor) {
        return assets.listAssets(actor);
    }

    @GetMapping(AssetRoutes.USERS)
    @PreAuthorize("hasAuthority('assets:assign')")
    public List<AssetUserSummary> listAssignableUsers() {
        return assets.listAssignableUsers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('assets:create')")
    public AssetDetailResponse create(@Valid @RequestBody AssetRequest request,
                                      @AuthenticationPrincipal UserAccount actor) {
        return assets.createAsset(request, actor);
    }

    @GetMapping(AssetRoutes.BY_ID)
    @PreAuthorize("hasAuthority('assets:read') or hasAuthority('assets:read_own')")
    public AssetDetailResponse get(@PathVariable Long assetId,
                                   @AuthenticationPrincipal UserAccount actor) {
        return assets.getAsset(assetId, actor);
    }

    @PutMapping(AssetRoutes.BY_ID)
    @PreAuthorize("hasAuthority('assets:update')")
    public AssetDetailResponse update(@PathVariable Long assetId, @Valid @RequestBody AssetRequest request,
                                      @AuthenticationPrincipal UserAccount actor) {
        return assets.updateAsset(assetId, request, actor);
    }

    @PostMapping(AssetRoutes.ASSIGNMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('assets:assign')")
    public AssetDetailResponse assign(@PathVariable Long assetId,
                                      @Valid @RequestBody AssetAssignmentRequest request,
                                      @AuthenticationPrincipal UserAccount actor) {
        return assets.assignAsset(assetId, request, actor);
    }

    @PostMapping(AssetRoutes.RETURN)
    @PreAuthorize("hasAuthority('assets:assign')")
    public AssetDetailResponse returnAsset(@PathVariable Long assetId, @PathVariable Long assignmentId,
                                           @Valid @RequestBody AssetReturnRequest request,
                                           @AuthenticationPrincipal UserAccount actor) {
        return assets.returnAsset(assetId, assignmentId, request, actor);
    }

    @PostMapping(AssetRoutes.MAINTENANCE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('assets:maintenance')")
    public AssetDetailResponse startMaintenance(@PathVariable Long assetId,
                                                @Valid @RequestBody AssetMaintenanceRequest request,
                                                @AuthenticationPrincipal UserAccount actor) {
        return assets.startMaintenance(assetId, request, actor);
    }

    @PatchMapping(AssetRoutes.MAINTENANCE_BY_ID)
    @PreAuthorize("hasAuthority('assets:maintenance')")
    public AssetDetailResponse changeMaintenanceStatus(@PathVariable Long assetId,
                                                        @PathVariable Long maintenanceId,
                                                        @Valid @RequestBody AssetMaintenanceStatusRequest request,
                                                        @AuthenticationPrincipal UserAccount actor) {
        return assets.changeMaintenanceStatus(assetId, maintenanceId, request, actor);
    }

    @PostMapping(value = AssetRoutes.DOCUMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('assets:document')")
    public AssetDetailResponse uploadDocument(@PathVariable Long assetId,
                                              @RequestParam("type") AssetDocumentType type,
                                              @RequestParam("file") MultipartFile file,
                                              @AuthenticationPrincipal UserAccount actor) {
        return assets.addDocument(assetId, type, file, actor);
    }

    @GetMapping(AssetRoutes.DOCUMENT_DOWNLOAD)
    @PreAuthorize("hasAuthority('assets:read') or hasAuthority('assets:read_own')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long assetId,
                                                      @PathVariable Long documentId,
                                                      @AuthenticationPrincipal UserAccount actor) {
        AssetDownload download = assets.downloadDocument(assetId, documentId, actor);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(download.path()));
    }

    @DeleteMapping(AssetRoutes.DOCUMENT_BY_ID)
    @PreAuthorize("hasAuthority('assets:document')")
    public AssetDetailResponse deleteDocument(@PathVariable Long assetId, @PathVariable Long documentId,
                                              @AuthenticationPrincipal UserAccount actor) {
        return assets.deleteDocument(assetId, documentId, actor);
    }

    @PatchMapping(AssetRoutes.RETIRE)
    @PreAuthorize("hasAuthority('assets:delete')")
    public AssetDetailResponse retire(@PathVariable Long assetId,
                                      @AuthenticationPrincipal UserAccount actor) {
        return assets.retireAsset(assetId, actor);
    }
}
