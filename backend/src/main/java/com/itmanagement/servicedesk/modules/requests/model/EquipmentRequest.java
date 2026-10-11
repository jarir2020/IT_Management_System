package com.itmanagement.servicedesk.modules.requests.model;

import com.itmanagement.servicedesk.modules.assets.model.AssetCategory;
import com.itmanagement.servicedesk.modules.departments.model.Department;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "equipment_requests")
public class EquipmentRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserAccount requester;

    // Keep the department from submission so later staff transfers do not change the approver scope.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssetCategory category;

    @Column(name = "item_name", nullable = false, length = 140)
    private String itemName;

    @Column(nullable = false, length = 5000)
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private EquipmentRequestStatus status = EquipmentRequestStatus.PENDING_APPROVAL;

    @Column(name = "decision_note", length = 2000)
    private String decisionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private UserAccount reviewedBy;

    @Column(name = "procurement_vendor", length = 100)
    private String procurementVendor;

    @Column(name = "purchase_reference", length = 100)
    private String purchaseReference;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "procurement_notes", length = 2000)
    private String procurementNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procurement_updated_by")
    private UserAccount procurementUpdatedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "allocated_asset_id")
    private Long allocatedAssetId;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected EquipmentRequest() {
    }

    public EquipmentRequest(UserAccount requester, Department department, AssetCategory category,
                            String itemName, String justification) {
        this.requester = requester;
        this.department = department;
        this.category = category;
        this.itemName = itemName;
        this.justification = justification;
        this.status = EquipmentRequestStatus.PENDING_APPROVAL;
    }

    public void approve(UserAccount actor, String note) {
        reviewedBy = actor;
        decisionNote = note;
        decidedAt = Instant.now();
        status = EquipmentRequestStatus.APPROVED;
    }

    public void reject(UserAccount actor, String note) {
        reviewedBy = actor;
        decisionNote = note;
        decidedAt = Instant.now();
        status = EquipmentRequestStatus.REJECTED;
    }

    public void sendToProcurement(UserAccount actor, String reason) {
        procurementUpdatedBy = actor;
        procurementNotes = appendNote(procurementNotes, "Stock unavailable", reason);
        status = EquipmentRequestStatus.PENDING_PROCUREMENT;
    }

    public void recordPurchase(UserAccount actor, String vendor, String reference, BigDecimal cost,
                               LocalDate expectedDeliveryDate, String notes) {
        procurementUpdatedBy = actor;
        procurementVendor = vendor;
        purchaseReference = reference;
        purchaseCost = cost;
        this.expectedDeliveryDate = expectedDeliveryDate;
        procurementNotes = appendNote(procurementNotes, "Purchase", notes);
        status = EquipmentRequestStatus.PURCHASING;
    }

    public void markReceived(UserAccount actor, String notes) {
        procurementUpdatedBy = actor;
        procurementNotes = appendNote(procurementNotes, "Receipt", notes);
        receivedAt = Instant.now();
        status = EquipmentRequestStatus.READY_FOR_ALLOCATION;
    }

    public void fulfill(Long assetId) {
        allocatedAssetId = assetId;
        fulfilledAt = Instant.now();
        status = EquipmentRequestStatus.FULFILLED;
    }

    public void cancel() {
        status = EquipmentRequestStatus.CANCELLED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    private static String appendNote(String current, String label, String note) {
        if (note == null || note.isBlank()) return current;
        String entry = label + ": " + note.trim();
        if (current == null || current.isBlank()) return entry;
        return (current + "\n" + entry).substring(0, Math.min(current.length() + entry.length() + 1, 2000));
    }

    public Long getId() { return id; }
    public UserAccount getRequester() { return requester; }
    public Department getDepartment() { return department; }
    public AssetCategory getCategory() { return category; }
    public String getItemName() { return itemName; }
    public String getJustification() { return justification; }
    public EquipmentRequestStatus getStatus() { return status; }
    public String getDecisionNote() { return decisionNote; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public String getProcurementVendor() { return procurementVendor; }
    public String getPurchaseReference() { return purchaseReference; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public LocalDate getExpectedDeliveryDate() { return expectedDeliveryDate; }
    public String getProcurementNotes() { return procurementNotes; }
    public UserAccount getProcurementUpdatedBy() { return procurementUpdatedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public Long getAllocatedAssetId() { return allocatedAssetId; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getFulfilledAt() { return fulfilledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
