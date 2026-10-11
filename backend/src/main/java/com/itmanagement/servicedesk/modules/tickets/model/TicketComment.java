package com.itmanagement.servicedesk.modules.tickets.model;

import com.itmanagement.servicedesk.modules.users.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ticket_comments")
public class TicketComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @Column(nullable = false, length = 10000)
    private String message;

    @Column(name = "internal_note", nullable = false)
    private boolean internalNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TicketComment() {
    }

    public TicketComment(Ticket ticket, UserAccount author, String message, boolean internalNote) {
        this.ticket = ticket;
        this.author = author;
        this.message = message;
        this.internalNote = internalNote;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public UserAccount getAuthor() { return author; }
    public String getMessage() { return message; }
    public boolean isInternalNote() { return internalNote; }
    public Instant getCreatedAt() { return createdAt; }
}
