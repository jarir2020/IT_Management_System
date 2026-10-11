package com.itmanagement.servicedesk.modules.tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.itmanagement.servicedesk.modules.audit.service.AuditService;
import com.itmanagement.servicedesk.modules.tickets.model.Ticket;
import com.itmanagement.servicedesk.modules.tickets.model.TicketStatus;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketAttachmentRepository;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketCommentRepository;
import com.itmanagement.servicedesk.modules.tickets.repository.TicketRepository;
import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import com.itmanagement.servicedesk.modules.users.repository.UserAccountRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

class TicketServiceAuthorizationTest {
    private final TicketRepository tickets = mock(TicketRepository.class);
    private final TicketCommentRepository comments = mock(TicketCommentRepository.class);
    private final TicketAttachmentRepository attachments = mock(TicketAttachmentRepository.class);
    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final TicketFileStorage fileStorage = mock(TicketFileStorage.class);
    private final AuditService audit = mock(AuditService.class);
    private final TicketSlaCalculator slaCalculator = new TicketSlaCalculator();
    @SuppressWarnings("unchecked")
    private final ObjectProvider<TicketEmailNotificationService> emailNotifications = mock(ObjectProvider.class);

    private TicketService service;

    @BeforeEach
    void setUp() {
        service = new TicketService(tickets, comments, attachments, users, fileStorage,
                audit, slaCalculator, emailNotifications);
    }

    @Test
    void requesterWithoutReadAllCannotReadAnotherUsersTicket() {
        UserAccount owner = account(10L, List.of());
        UserAccount viewer = account(20L, List.of(new SimpleGrantedAuthority("tickets:read")));
        Ticket ticket = mock(Ticket.class);
        when(ticket.getRequester()).thenReturn(owner);
        when(tickets.findDetailedById(42L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.getTicket(42L, viewer))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void assignedAgentCannotSkipTicketWorkflowStates() {
        UserAccount agent = account(10L, List.of(new SimpleGrantedAuthority("tickets:status")));
        Ticket ticket = mock(Ticket.class);
        when(ticket.getRequester()).thenReturn(agent);
        when(ticket.getAssignee()).thenReturn(agent);
        when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
        when(tickets.findDetailedById(42L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.changeStatus(42L, TicketStatus.RESOLVED, agent))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(ticket, never()).changeStatus(TicketStatus.RESOLVED);
    }

    @Test
    void supportPermissionDoesNotLetAnUnassignedAgentManageTicket() {
        UserAccount requester = account(10L, List.of());
        UserAccount agent = account(20L, List.of(
                new SimpleGrantedAuthority("tickets:read_all"),
                new SimpleGrantedAuthority("tickets:status")));
        Ticket ticket = mock(Ticket.class);
        when(ticket.getRequester()).thenReturn(requester);
        when(ticket.getAssignee()).thenReturn(null);
        when(ticket.getStatus()).thenReturn(TicketStatus.OPEN);
        when(tickets.findDetailedById(42L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.changeStatus(42L, TicketStatus.IN_PROGRESS, agent))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(ticket, never()).changeStatus(TicketStatus.IN_PROGRESS);
    }

    private static UserAccount account(Long id, Collection<? extends GrantedAuthority> authorities) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(id);
        doReturn(List.copyOf(authorities)).when(account).getAuthorities();
        return account;
    }
}
