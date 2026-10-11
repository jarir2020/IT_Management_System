CREATE TABLE assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_tag VARCHAR(48) NOT NULL,
    name VARCHAR(140) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description VARCHAR(5000),
    manufacturer VARCHAR(100),
    model_number VARCHAR(100),
    serial_number VARCHAR(100),
    purchase_date DATE,
    purchase_cost DECIMAL(12,2),
    warranty_expires_on DATE,
    status VARCHAR(24) NOT NULL DEFAULT 'AVAILABLE',
    assigned_to BIGINT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_assets_asset_tag UNIQUE (asset_tag),
    CONSTRAINT uq_assets_serial_number UNIQUE (serial_number),
    CONSTRAINT fk_assets_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_assets_status_updated (status, updated_at),
    INDEX idx_assets_assigned_to (assigned_to)
);

CREATE TABLE asset_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    assigned_to BIGINT NOT NULL,
    assigned_by BIGINT NOT NULL,
    returned_by BIGINT,
    assignment_note VARCHAR(1000),
    return_condition VARCHAR(2000),
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    returned_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_asset_assignments_asset FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_asset_assignments_assigned_to FOREIGN KEY (assigned_to) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_asset_assignments_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_asset_assignments_returned_by FOREIGN KEY (returned_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_asset_assignments_history (asset_id, assigned_at),
    INDEX idx_asset_assignments_active (asset_id, returned_at)
);

CREATE TABLE asset_maintenance (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    summary VARCHAR(180) NOT NULL,
    vendor VARCHAR(100),
    notes VARCHAR(5000),
    cost DECIMAL(12,2),
    started_on DATE NOT NULL,
    completed_on DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_asset_maintenance_asset FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_asset_maintenance_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_asset_maintenance_history (asset_id, created_at)
);

CREATE TABLE asset_documents (
    id BIGINT NOT NULL AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(80) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_asset_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_asset_documents_asset FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE RESTRICT,
    CONSTRAINT fk_asset_documents_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_asset_documents_asset_uploaded (asset_id, uploaded_at)
);
