package com.itmanagement.servicedesk.modules.assets.model;

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
@Table(name = "asset_assignments")
public class AssetAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_to", nullable = false)
    private UserAccount assignedTo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by", nullable = false)
    private UserAccount assignedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "returned_by")
    private UserAccount returnedBy;

    @Column(name = "equipment_request_id")
    private Long equipmentRequestId;

    @Column(name = "assignment_note", length = 1000)
    private String assignmentNote;

    @Column(name = "return_condition", length = 2000)
    private String returnCondition;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    protected AssetAssignment() {
    }

    public AssetAssignment(Asset asset, UserAccount assignedTo, UserAccount assignedBy, String assignmentNote) {
        this(asset, assignedTo, assignedBy, assignmentNote, null);
    }

    public AssetAssignment(Asset asset, UserAccount assignedTo, UserAccount assignedBy,
                           String assignmentNote, Long equipmentRequestId) {
        this.asset = asset;
        this.assignedTo = assignedTo;
        this.assignedBy = assignedBy;
        this.assignmentNote = assignmentNote;
        this.equipmentRequestId = equipmentRequestId;
    }

    public void markReturned(UserAccount actor, String returnCondition) {
        returnedBy = actor;
        this.returnCondition = returnCondition;
        returnedAt = Instant.now();
    }

    @PrePersist
    void onCreate() { assignedAt = Instant.now(); }

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public UserAccount getAssignedTo() { return assignedTo; }
    public UserAccount getAssignedBy() { return assignedBy; }
    public UserAccount getReturnedBy() { return returnedBy; }
    public Long getEquipmentRequestId() { return equipmentRequestId; }
    public String getAssignmentNote() { return assignmentNote; }
    public String getReturnCondition() { return returnCondition; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getReturnedAt() { return returnedAt; }
}
