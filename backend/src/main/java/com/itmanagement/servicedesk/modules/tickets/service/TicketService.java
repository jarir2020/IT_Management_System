package com.itmanagement.servicedesk.modules.tickets.service;

import com.itmanagement.servicedesk.modules.audit.service.AuditService;
import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import com.itmanagement.servicedesk.modules.tickets.model.TicketAttachment;
import com.itmanagement.servicedesk.modules.tickets.model.TicketAttachmentResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketAssigneeRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketCategory;
import com.itmanagement.servicedesk.modules.tickets.model.TicketComment;
import com.itmanagement.servicedesk.modules.tickets.model.TicketCommentRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketCommentResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketCreateRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketDetailResponse;
import com.itmanagement.servicedesk.modules.tickets.model.TicketPriority;
import com.itmanagement.servicedesk.modules.tickets.model.TicketStatus;
import com.itmanagement.servicedesk.modules.tickets.model.TicketSummary;
import com.itmanagement.servicedesk.modules.tickets.model.TicketUpdateRequest;
import com.itmanagement.servicedesk.modules.tickets.model.TicketUserSummary;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketAttachmentRepository;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketCommentRepository;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.users.repository.UserAccountRepository;
import java.nio.file.Path;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketAttachmentRepository attachments;
    private final UserAccountRepository users;
    private final TicketFileStorage fileStorage;
    private final AuditService audit;
    private final TicketSlaCalculator slaCalculator;
    private final ObjectProvider<TicketEmailNotificationService> emailNotifications;

    public TicketService(TicketRepository tickets,
                         TicketCommentRepository comments,
                         TicketAttachmentRepository attachments,
                         UserAccountRepository users,
                         TicketFileStorage fileStorage,
                         AuditService audit,
                         TicketSlaCalculator slaCalculator,
                         ObjectProvider<TicketEmailNotificationService> emailNotifications) {
        this.tickets = tickets;
        this.comments = comments;
        this.attachments = attachments;
        this.users = users;
        this.fileStorage = fileStorage;
        this.audit = audit;
        this.slaCalculator = slaCalculator;
        this.emailNotifications = emailNotifications;
    }

    @Transactional(readOnly = true)
    public List<TicketSummary> listTickets(UserAccount actor) {
        List<Ticket> visibleTickets = hasAuthority(actor, "tickets:read_all")
                ? tickets.findAllDetailed()
                : tickets.findAllForRequester(actor.getId());
        return visibleTickets.stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketUserSummary> listSupportAgents() {
        return users.findActiveSupportAgents().stream().map(TicketService::toUserSummary).toList();
    }

    @Transactional
    public TicketDetailResponse createTicket(TicketCreateRequest request, UserAccount actor) {
        Ticket ticket = new Ticket(actor, request.title().trim(), request.description().trim(),
                request.category(), request.priority());
        Ticket saved = tickets.save(ticket);
        audit.record(actor, "TICKET_CREATED", "TICKET", saved.getId(), "Created a support ticket");
        afterCommit(() -> emailNotifications.ifAvailable(mail ->
                mail.ticketCreated(saved.getId(), saved.getTitle(), actor.getEmail())));
        return toDetail(saved, actor);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getTicket(Long ticketId, UserAccount actor) {
        return toDetail(findReadableTicket(ticketId, actor), actor);
    }

    @Transactional
    public TicketDetailResponse updateTicket(Long ticketId, TicketUpdateRequest request, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        boolean requesterCanEdit = ticket.getRequester().getId().equals(actor.getId())
                && ticket.getStatus() == TicketStatus.OPEN;
        boolean assignedAgentCanEdit = hasAuthority(actor, "tickets:update") && canManage(ticket, actor);
        if (!requesterCanEdit && !assignedAgentCanEdit) {
            throw forbidden("You can edit an open ticket you created or a ticket assigned to you");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw conflict("Closed tickets cannot be changed");
        }

        ticket.updateDetails(request.title().trim(), request.description().trim(),
                request.category(), request.priority());
        audit.record(actor, "TICKET_UPDATED", "TICKET", ticket.getId(), "Updated ticket details");
        return toDetail(ticket, actor);
    }

    @Transactional
    public TicketDetailResponse assignTicket(Long ticketId, TicketAssigneeRequest request, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        requireCanManage(ticket, actor);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw conflict("Closed tickets cannot be reassigned");
        }

        UserAccount oldAssignee = ticket.getAssignee();
        UserAccount assignee = request.userId() == null ? null : users.findDetailedById(request.userId())
                .orElseThrow(() -> badRequest("The selected support account does not exist"));
        if (assignee != null && (!assignee.isEnabled() || !isSupportAccount(assignee))) {
            throw badRequest("Tickets can only be assigned to an active support agent or administrator");
        }
        ticket.assignTo(assignee);
        if (oldAssignee == null ? assignee != null : !oldAssignee.getId().equals(assignee == null ? null : assignee.getId())) {
            audit.record(actor, "TICKET_ASSIGNED", "TICKET", ticket.getId(),
                    assignee == null ? "Removed ticket assignment" : "Assigned ticket to support staff");
            if (assignee != null) {
                afterCommit(() -> emailNotifications.ifAvailable(mail ->
                        mail.ticketAssigned(ticket.getId(), ticket.getTitle(), assignee.getEmail())));
            }
        }
        return toDetail(ticket, actor);
    }

    @Transactional
    public TicketDetailResponse changeStatus(Long ticketId, TicketStatus nextStatus, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        if (nextStatus == ticket.getStatus()) return toDetail(ticket, actor);

        boolean requesterClosingResolvedTicket = nextStatus == TicketStatus.CLOSED
                && ticket.getStatus() == TicketStatus.RESOLVED
                && ticket.getRequester().getId().equals(actor.getId());
        if (!requesterClosingResolvedTicket) {
            if (!hasAuthority(actor, "tickets:status")) {
                throw forbidden("You cannot move this ticket through the support workflow");
            }
            requireCanManage(ticket, actor);
            if (!isAllowedTransition(ticket.getStatus(), nextStatus)) {
                throw conflict("Tickets can move from Open to In Progress, In Progress to Resolved, and Resolved to Closed");
            }
        }

        TicketStatus previousStatus = ticket.getStatus();
        ticket.changeStatus(nextStatus);
        audit.record(actor, "TICKET_STATUS_CHANGED", "TICKET", ticket.getId(),
                "Changed ticket status from " + previousStatus + " to " + nextStatus);
        afterCommit(() -> emailNotifications.ifAvailable(mail ->
                mail.ticketStatusChanged(ticket.getId(), ticket.getTitle(), nextStatus.name(),
                        ticket.getRequester().getEmail())));
        return toDetail(ticket, actor);
    }

    @Transactional
    public TicketDetailResponse addComment(Long ticketId, TicketCommentRequest request, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        if (request.internalNote() && !hasAuthority(actor, "tickets:comment_internal")) {
            throw forbidden("Only support staff can add internal notes");
        }
        comments.save(new TicketComment(ticket, actor, request.message().trim(), request.internalNote()));
        boolean requesterComment = ticket.getRequester().getId().equals(actor.getId());
        if (!request.internalNote() && !requesterComment && hasAuthority(actor, "tickets:read_all")) {
            ticket.markFirstResponse(java.time.Instant.now());
        }
        audit.record(actor, request.internalNote() ? "TICKET_INTERNAL_NOTE_ADDED" : "TICKET_COMMENT_ADDED",
                "TICKET", ticket.getId(), request.internalNote() ? "Added an internal ticket note" : "Added a public ticket update");
        if (!request.internalNote()) {
            UserAccount recipient = requesterComment ? ticket.getAssignee() : ticket.getRequester();
            if (recipient != null) {
                afterCommit(() -> emailNotifications.ifAvailable(mail ->
                        mail.ticketCommented(ticket.getId(), ticket.getTitle(), recipient.getEmail())));
            }
        }
        return toDetail(ticket, actor);
    }

    @Transactional
    public TicketDetailResponse addAttachment(Long ticketId, MultipartFile file, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw conflict("Closed tickets cannot receive new attachments");
        }
        boolean requesterOwns = ticket.getRequester().getId().equals(actor.getId());
        boolean assignedAgent = hasAuthority(actor, "tickets:attach") && canManage(ticket, actor);
        if (!requesterOwns && !assignedAgent) {
            throw forbidden("Only the requester or assigned support staff can attach files");
        }

        TicketFileStorage.StoredFile storedFile = fileStorage.store(file);
        removeStoredFileIfTransactionRollsBack(storedFile.storageKey());
        attachments.save(new TicketAttachment(ticket, actor, storedFile.originalFilename(),
                storedFile.storageKey(), storedFile.sizeBytes()));
        return toDetail(ticket, actor);
    }

    @Transactional(readOnly = true)
    public TicketDownload downloadAttachment(Long ticketId, Long attachmentId, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        TicketAttachment attachment = attachments.findByIdAndTicketId(attachmentId, ticket.getId())
                .orElseThrow(() -> notFound("Attachment not found"));
        Path path = fileStorage.pathFor(attachment.getStorageKey());
        return new TicketDownload(path, attachment.getOriginalFilename(), attachment.getSizeBytes());
    }

    @Transactional
    public TicketDetailResponse deleteAttachment(Long ticketId, Long attachmentId, UserAccount actor) {
        Ticket ticket = findReadableTicket(ticketId, actor);
        TicketAttachment attachment = attachments.findByIdAndTicketId(attachmentId, ticket.getId())
                .orElseThrow(() -> notFound("Attachment not found"));
        boolean canDelete = hasAuthority(actor, "tickets:delete")
                || attachment.getUploadedBy().getId().equals(actor.getId())
                || (hasAuthority(actor, "tickets:update") && canManage(ticket, actor));
        if (!canDelete) throw forbidden("Only the uploader or assigned support staff can remove this attachment");

        String storageKey = attachment.getStorageKey();
        attachments.delete(attachment);
        deleteStoredFileAfterCommit(storageKey);
        return toDetail(ticket, actor);
    }

    @Transactional
    public void deleteTicket(Long ticketId, UserAccount actor) {
        if (!hasAuthority(actor, "tickets:delete")) {
            throw forbidden("Only administrators can delete tickets");
        }
        Ticket ticket = tickets.findDetailedById(ticketId)
                .orElseThrow(() -> notFound("Ticket not found"));
        attachments.findAllByTicketIdOrderByUploadedAtAsc(ticket.getId())
                .forEach(attachment -> deleteStoredFileAfterCommit(attachment.getStorageKey()));
        audit.record(actor, "TICKET_DELETED", "TICKET", ticket.getId(), "Deleted a support ticket");
        tickets.delete(ticket);
    }

    private Ticket findReadableTicket(Long ticketId, UserAccount actor) {
        Ticket ticket = tickets.findDetailedById(ticketId)
                .orElseThrow(() -> notFound("Ticket not found"));
        if (!ticket.getRequester().getId().equals(actor.getId())
                && !hasAuthority(actor, "tickets:read_all")) {
            throw notFound("Ticket not found");
        }
        return ticket;
    }

    @Transactional(readOnly = true)
    public void assertCanReadTicket(Long ticketId, UserAccount actor) {
        findReadableTicket(ticketId, actor);
    }

    private void requireCanManage(Ticket ticket, UserAccount actor) {
        if (!canManage(ticket, actor)) {
            throw forbidden("Only the assigned support agent or an administrator can manage this ticket");
        }
    }

    private static boolean canManage(Ticket ticket, UserAccount actor) {
        return hasAuthority(actor, "tickets:delete")
                || (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(actor.getId()));
    }

    private static boolean hasAuthority(UserAccount actor, String authority) {
        return actor.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(authority::equals);
    }

    private static boolean isSupportAccount(UserAccount user) {
        return user.getRoles().stream().anyMatch(role ->
                role.getCode().equals("SUPPORT_AGENT") || role.getCode().equals("ADMIN"));
    }

    private static boolean isAllowedTransition(TicketStatus current, TicketStatus next) {
        return (current == TicketStatus.OPEN && next == TicketStatus.IN_PROGRESS)
                || (current == TicketStatus.IN_PROGRESS && next == TicketStatus.RESOLVED)
                || (current == TicketStatus.RESOLVED && next == TicketStatus.CLOSED);
    }

    private TicketSummary toSummary(Ticket ticket) {
        return new TicketSummary(ticket.getId(), ticket.getTitle(), ticket.getCategory(), ticket.getPriority(),
                ticket.getStatus(), toUserSummary(ticket.getRequester()), toUserSummary(ticket.getAssignee()),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), slaCalculator.summarize(ticket));
    }

    private TicketDetailResponse toDetail(Ticket ticket, UserAccount viewer) {
        List<TicketCommentResponse> visibleComments = comments.findAllByTicketIdOrderByCreatedAtAsc(ticket.getId())
                .stream()
                .filter(comment -> !comment.isInternalNote() || hasAuthority(viewer, "tickets:comment_internal"))
                .map(comment -> new TicketCommentResponse(comment.getId(), comment.getMessage(),
                        comment.isInternalNote(), toUserSummary(comment.getAuthor()), comment.getCreatedAt()))
                .toList();
        List<TicketAttachmentResponse> ticketAttachments = attachments
                .findAllByTicketIdOrderByUploadedAtAsc(ticket.getId()).stream()
                .map(attachment -> new TicketAttachmentResponse(attachment.getId(),
                        attachment.getOriginalFilename(), attachment.getSizeBytes(),
                        toUserSummary(attachment.getUploadedBy()), attachment.getUploadedAt()))
                .toList();

        return new TicketDetailResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
                ticket.getCategory(), ticket.getPriority(), ticket.getStatus(),
                toUserSummary(ticket.getRequester()), toUserSummary(ticket.getAssignee()),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt(), ticket.getClosedAt(),
                slaCalculator.summarize(ticket), visibleComments, ticketAttachments);
    }

    private static TicketUserSummary toUserSummary(UserAccount user) {
        if (user == null) return null;
        return new TicketUserSummary(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getDepartment() == null ? null : user.getDepartment().getName());
    }

    private void removeStoredFileIfTransactionRollsBack(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
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
            public void afterCommit() {
                fileStorage.deleteQuietly(storageKey);
            }
        });
    }

    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { action.run(); }
        });
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
