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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "assets")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_tag", nullable = false, length = 48, unique = true)
    private String assetTag;

    @Column(nullable = false, length = 140)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AssetCategory category;

    @Column(length = 5000)
    private String description;

    @Column(length = 100)
    private String manufacturer;

    @Column(name = "model_number", length = 100)
    private String modelNumber;

    @Column(name = "serial_number", length = 100, unique = true)
    private String serialNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_cost", precision = 12, scale = 2)
    private BigDecimal purchaseCost;

    @Column(name = "warranty_expires_on")
    private LocalDate warrantyExpiresOn;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    @Column(name = "salvage_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AssetStatus status = AssetStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private UserAccount assignedTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long version;

    protected Asset() {
    }

    public Asset(String assetTag, String name, AssetCategory category, String description,
                 String manufacturer, String modelNumber, String serialNumber, LocalDate purchaseDate,
                 BigDecimal purchaseCost, LocalDate warrantyExpiresOn,
                 Integer usefulLifeMonths, BigDecimal salvageValue) {
        updateDetails(assetTag, name, category, description, manufacturer, modelNumber,
                serialNumber, purchaseDate, purchaseCost, warrantyExpiresOn, usefulLifeMonths, salvageValue);
    }

    public void updateDetails(String assetTag, String name, AssetCategory category, String description,
                              String manufacturer, String modelNumber, String serialNumber,
                              LocalDate purchaseDate, BigDecimal purchaseCost, LocalDate warrantyExpiresOn,
                              Integer usefulLifeMonths, BigDecimal salvageValue) {
        this.assetTag = assetTag;
        this.name = name;
        this.category = category;
        this.description = description;
        this.manufacturer = manufacturer;
        this.modelNumber = modelNumber;
        this.serialNumber = serialNumber;
        this.purchaseDate = purchaseDate;
        this.purchaseCost = purchaseCost;
        this.warrantyExpiresOn = warrantyExpiresOn;
        this.usefulLifeMonths = usefulLifeMonths;
        this.salvageValue = salvageValue == null ? BigDecimal.ZERO : salvageValue;
    }

    public void assignTo(UserAccount user) {
        assignedTo = user;
        status = AssetStatus.ASSIGNED;
    }

    public void returnToInventory(boolean needsMaintenance) {
        assignedTo = null;
        status = needsMaintenance ? AssetStatus.UNDER_MAINTENANCE : AssetStatus.AVAILABLE;
    }

    public void startMaintenance() {
        status = AssetStatus.UNDER_MAINTENANCE;
    }

    public void finishMaintenance() {
        status = AssetStatus.AVAILABLE;
    }

    public void retire() {
        assignedTo = null;
        status = AssetStatus.RETIRED;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getAssetTag() { return assetTag; }
    public String getName() { return name; }
    public AssetCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getManufacturer() { return manufacturer; }
    public String getModelNumber() { return modelNumber; }
    public String getSerialNumber() { return serialNumber; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public BigDecimal getPurchaseCost() { return purchaseCost; }
    public LocalDate getWarrantyExpiresOn() { return warrantyExpiresOn; }
    public Integer getUsefulLifeMonths() { return usefulLifeMonths; }
    public BigDecimal getSalvageValue() { return salvageValue; }
    public AssetStatus getStatus() { return status; }
    public UserAccount getAssignedTo() { return assignedTo; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
