package com.itmanagement.servicedesk.modules.assets.model;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "asset_maintenance")
public class AssetMaintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private UserAccount createdBy;

    @Column(nullable = false, length = 180)
    private String summary;

    @Column(length = 100)
    private String vendor;

    @Column(length = 5000)
    private String notes;

    @Column(precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MaintenanceStatus status = MaintenanceStatus.IN_PROGRESS;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AssetMaintenance() {
    }

    public AssetMaintenance(Asset asset, UserAccount createdBy, String summary, String vendor,
                            String notes, BigDecimal cost, LocalDate startedOn) {
        this.asset = asset;
        this.createdBy = createdBy;
        this.summary = summary;
        this.vendor = vendor;
        this.notes = notes;
        this.cost = cost;
        this.startedOn = startedOn;
    }

    public void changeStatus(MaintenanceStatus status) {
        this.status = status;
        completedOn = status == MaintenanceStatus.COMPLETED ? LocalDate.now() : null;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Asset getAsset() { return asset; }
    public UserAccount getCreatedBy() { return createdBy; }
    public String getSummary() { return summary; }
    public String getVendor() { return vendor; }
    public String getNotes() { return notes; }
    public BigDecimal getCost() { return cost; }
    public LocalDate getStartedOn() { return startedOn; }
    public LocalDate getCompletedOn() { return completedOn; }
    public MaintenanceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
