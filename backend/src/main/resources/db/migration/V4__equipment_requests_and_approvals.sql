CREATE TABLE equipment_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    department_id BIGINT,
    category VARCHAR(32) NOT NULL,
    item_name VARCHAR(140) NOT NULL,
    justification VARCHAR(5000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_APPROVAL',
    decision_note VARCHAR(2000),
    reviewed_by BIGINT,
    procurement_vendor VARCHAR(100),
    purchase_reference VARCHAR(100),
    purchase_cost DECIMAL(12,2),
    expected_delivery_date DATE,
    procurement_notes VARCHAR(2000),
    procurement_updated_by BIGINT,
    received_at TIMESTAMP(6),
    allocated_asset_id BIGINT,
    decided_at TIMESTAMP(6),
    fulfilled_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_equipment_requests_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_equipment_requests_department FOREIGN KEY (department_id) REFERENCES departments (id) ON DELETE SET NULL,
    CONSTRAINT fk_equipment_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_equipment_requests_procurement_updated_by FOREIGN KEY (procurement_updated_by) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_equipment_requests_allocated_asset FOREIGN KEY (allocated_asset_id) REFERENCES assets (id) ON DELETE RESTRICT,
    INDEX idx_equipment_requests_requester (requester_id, updated_at),
    INDEX idx_equipment_requests_department (department_id, status, updated_at),
    INDEX idx_equipment_requests_status (status, updated_at)
);

ALTER TABLE asset_assignments
    ADD COLUMN equipment_request_id BIGINT NULL,
    ADD CONSTRAINT fk_asset_assignments_equipment_request
        FOREIGN KEY (equipment_request_id) REFERENCES equipment_requests (id) ON DELETE RESTRICT,
    ADD INDEX idx_asset_assignments_equipment_request (equipment_request_id);
