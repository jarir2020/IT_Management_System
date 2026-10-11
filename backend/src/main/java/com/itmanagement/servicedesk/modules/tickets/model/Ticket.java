package com.itmanagement.servicedesk.modules.tickets.model;

import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserAccount requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserAccount assignee;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected Ticket() {
    }

    public Ticket(UserAccount requester, String title, String description,
                  TicketCategory category, TicketPriority priority) {
        this.requester = requester;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = TicketStatus.OPEN;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UserAccount getRequester() { return requester; }
    public UserAccount getAssignee() { return assignee; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketCategory getCategory() { return category; }
    public TicketPriority getPriority() { return priority; }
    public TicketStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getFirstRespondedAt() { return firstRespondedAt; }
    public Instant getClosedAt() { return closedAt; }

    public void updateDetails(String title, String description, TicketCategory category, TicketPriority priority) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
    }

    public void assignTo(UserAccount assignee) {
        this.assignee = assignee;
    }

    public void changeStatus(TicketStatus status) {
        Instant now = Instant.now();
        this.status = status;
        if (status == TicketStatus.IN_PROGRESS) markFirstResponse(now);
        if (status == TicketStatus.RESOLVED) resolvedAt = now;
        if (status == TicketStatus.CLOSED) closedAt = now;
    }

    public void markFirstResponse(Instant at) {
        if (firstRespondedAt == null) firstRespondedAt = at;
    }
}
