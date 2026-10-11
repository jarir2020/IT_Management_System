package com.itmanagement.servicedesk.modules.tickets.controller;

import com.itmanagement.servicedesk.modules.tickets.model.TicketAssigneeRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketChatMessageResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketCommentRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketCreateRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketDetailResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketStatusRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketSummary;
import com.itmanagement.servicedesk.modules.tickets.model.TicketUpdateRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketUserSummary;
import com.itmanagement.servicedesk.modules.tickets.routes.TicketRoutes;
import com.itmanagement.servicedesk.modules.tickets.service.TicketDownload;
import com.itmanagement.servicedesk.modules.tickets.service.TicketChatService;
import com.itmanagement.servicedesk.modules.tickets.service.TicketService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping(TicketRoutes.BASE_PATH)
public class TicketController {
    private final TicketService tickets;
    private final TicketChatService chat;

    public TicketController(TicketService tickets, TicketChatService chat) {
        this.tickets = tickets;
        this.chat = chat;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('tickets:read')")
    public List<TicketSummary> list(@AuthenticationPrincipal UserAccount actor) {
        return tickets.listTickets(actor);
    }

    @GetMapping(TicketRoutes.AGENTS)
    @PreAuthorize("hasAuthority('tickets:assign')")
    public List<TicketUserSummary> listSupportAgents() {
        return tickets.listSupportAgents();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tickets:create')")
    public TicketDetailResponse create(@Valid @RequestBody TicketCreateRequest request,
                                       @AuthenticationPrincipal UserAccount actor) {
        return tickets.createTicket(request, actor);
    }

    @GetMapping(TicketRoutes.BY_ID)
    @PreAuthorize("hasAuthority('tickets:read')")
    public TicketDetailResponse get(@PathVariable Long ticketId,
                                    @AuthenticationPrincipal UserAccount actor) {
        return tickets.getTicket(ticketId, actor);
    }

    @GetMapping(TicketRoutes.CHAT)
    @PreAuthorize("hasAuthority('tickets:read')")
    public List<TicketChatMessageResponse> recentChat(@PathVariable Long ticketId,
                                                       @AuthenticationPrincipal UserAccount actor) {
        return chat.recent(ticketId, actor);
    }

    @PutMapping(TicketRoutes.BY_ID)
    @PreAuthorize("hasAuthority('tickets:update') or hasAuthority('tickets:create')")
    public TicketDetailResponse update(@PathVariable Long ticketId,
                                       @Valid @RequestBody TicketUpdateRequest request,
                                       @AuthenticationPrincipal UserAccount actor) {
        return tickets.updateTicket(ticketId, request, actor);
    }

    @PatchMapping(TicketRoutes.ASSIGNEE)
    @PreAuthorize("hasAuthority('tickets:assign')")
    public TicketDetailResponse assign(@PathVariable Long ticketId,
                                       @RequestBody TicketAssigneeRequest request,
                                       @AuthenticationPrincipal UserAccount actor) {
        return tickets.assignTicket(ticketId, request, actor);
    }

    @PatchMapping(TicketRoutes.STATUS)
    @PreAuthorize("hasAuthority('tickets:status') or hasAuthority('tickets:create')")
    public TicketDetailResponse changeStatus(@PathVariable Long ticketId,
                                             @Valid @RequestBody TicketStatusRequest request,
                                             @AuthenticationPrincipal UserAccount actor) {
        return tickets.changeStatus(ticketId, request.status(), actor);
    }

    @PostMapping(TicketRoutes.COMMENTS)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tickets:comment')")
    public TicketDetailResponse comment(@PathVariable Long ticketId,
                                        @Valid @RequestBody TicketCommentRequest request,
                                        @AuthenticationPrincipal UserAccount actor) {
        return tickets.addComment(ticketId, request, actor);
    }

    @PostMapping(value = TicketRoutes.ATTACHMENTS, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('tickets:attach')")
    public TicketDetailResponse upload(@PathVariable Long ticketId,
                                       @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal UserAccount actor) {
        return tickets.addAttachment(ticketId, file, actor);
    }

    @GetMapping(TicketRoutes.ATTACHMENT_DOWNLOAD)
    @PreAuthorize("hasAuthority('tickets:read')")
    public ResponseEntity<Resource> download(@PathVariable Long ticketId,
                                             @PathVariable Long attachmentId,
                                             @AuthenticationPrincipal UserAccount actor) {
        TicketDownload download = tickets.downloadAttachment(ticketId, attachmentId, actor);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(download.path()));
    }

    @DeleteMapping(TicketRoutes.ATTACHMENT_BY_ID)
    @PreAuthorize("hasAuthority('tickets:attach')")
    public TicketDetailResponse deleteAttachment(@PathVariable Long ticketId,
                                                 @PathVariable Long attachmentId,
                                                 @AuthenticationPrincipal UserAccount actor) {
        return tickets.deleteAttachment(ticketId, attachmentId, actor);
    }

    @DeleteMapping(TicketRoutes.BY_ID)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('tickets:delete')")
    public void delete(@PathVariable Long ticketId, @AuthenticationPrincipal UserAccount actor) {
        tickets.deleteTicket(ticketId, actor);
    }
}
