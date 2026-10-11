package com.itmanagement.servicedesk.modules.access.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "permissions", uniqueConstraints =
        @UniqueConstraint(name = "uq_permissions_code", columnNames = "code"))
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 255)
    private String description;

    protected Permission() {
    }

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
