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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "ticket_attachments", uniqueConstraints =
        @UniqueConstraint(name = "uq_ticket_attachments_storage_key", columnNames = "storage_key"))
public class TicketAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private UserAccount uploadedBy;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "storage_key", nullable = false, length = 80)
    private String storageKey;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected TicketAttachment() {
    }

    public TicketAttachment(Ticket ticket, UserAccount uploadedBy, String originalFilename,
                            String storageKey, long sizeBytes) {
        this.ticket = ticket;
        this.uploadedBy = uploadedBy;
        this.originalFilename = originalFilename;
        this.storageKey = storageKey;
        this.sizeBytes = sizeBytes;
    }

    @PrePersist
    void onCreate() { uploadedAt = Instant.now(); }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public UserAccount getUploadedBy() { return uploadedBy; }
    public String getOriginalFilename() { return originalFilename; }
    public String getStorageKey() { return storageKey; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getUploadedAt() { return uploadedAt; }
}
