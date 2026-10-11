package com.itmanagement.servicedesk.modules.reports.controller;

import com.itmanagement.servicedesk.modules.reports.model.AssetImportResult;
import com.itmanagement.servicedesk.modules.reports.routes.ReportRoutes;
import com.itmanagement.servicedesk.modules.reports.service.ReportService;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ReportRoutes.BASE_PATH)
public class ReportController {
    private static final MediaType XLSX = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReportService reports;

    public ReportController(ReportService reports) {
        this.reports = reports;
    }

    @GetMapping(ReportRoutes.ASSETS_XLSX)
    @PreAuthorize("hasAuthority('reports:assets')")
    public ResponseEntity<ByteArrayResource> assetsXlsx(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.assetsXlsx(actor), XLSX, "assets.xlsx");
    }

    @GetMapping(ReportRoutes.ASSETS_PDF)
    @PreAuthorize("hasAuthority('reports:assets')")
    public ResponseEntity<ByteArrayResource> assetsPdf(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.assetsPdf(actor), MediaType.APPLICATION_PDF, "assets.pdf");
    }

    @GetMapping(ReportRoutes.TICKETS_XLSX)
    @PreAuthorize("hasAuthority('reports:tickets')")
    public ResponseEntity<ByteArrayResource> ticketsXlsx(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.ticketsXlsx(actor), XLSX, "tickets.xlsx");
    }

    @GetMapping(ReportRoutes.TICKETS_PDF)
    @PreAuthorize("hasAuthority('reports:tickets')")
    public ResponseEntity<ByteArrayResource> ticketsPdf(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.ticketsPdf(actor), MediaType.APPLICATION_PDF, "tickets.pdf");
    }

    @GetMapping(ReportRoutes.REQUESTS_XLSX)
    @PreAuthorize("hasAuthority('reports:requests') or hasAuthority('reports:requests_department')")
    public ResponseEntity<ByteArrayResource> requestsXlsx(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.requestsXlsx(actor), XLSX, "equipment-requests.xlsx");
    }

    @GetMapping(ReportRoutes.REQUESTS_PDF)
    @PreAuthorize("hasAuthority('reports:requests') or hasAuthority('reports:requests_department')")
    public ResponseEntity<ByteArrayResource> requestsPdf(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.requestsPdf(actor), MediaType.APPLICATION_PDF, "equipment-requests.pdf");
    }

    @GetMapping(ReportRoutes.BUNDLE_ZIP)
    @PreAuthorize("hasAuthority('reports:assets') or hasAuthority('reports:tickets') or hasAuthority('reports:requests') or hasAuthority('reports:requests_department')")
    public ResponseEntity<ByteArrayResource> bundle(@AuthenticationPrincipal UserAccount actor) {
        return attachment(reports.bundleZip(actor), MediaType.parseMediaType("application/zip"), "reports.zip");
    }

    @GetMapping(ReportRoutes.ASSET_IMPORT_TEMPLATE)
    @PreAuthorize("hasAuthority('reports:assets_import')")
    public ResponseEntity<ByteArrayResource> assetImportTemplate() {
        return attachment(reports.assetImportTemplate(), XLSX, "asset-import-template.xlsx");
    }

    @PostMapping(ReportRoutes.ASSET_IMPORT)
    @PreAuthorize("hasAuthority('reports:assets_import')")
    public ResponseEntity<AssetImportResult> importAssets(@RequestParam("file") MultipartFile file,
                                                           @AuthenticationPrincipal UserAccount actor) {
        return ResponseEntity.status(HttpStatus.OK).body(reports.importAssets(file, actor));
    }

    private static ResponseEntity<ByteArrayResource> attachment(byte[] bytes, MediaType mediaType, String filename) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ByteArrayResource(bytes));
    }
}
