CREATE TABLE tickets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    assignee_id BIGINT,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(10000) NOT NULL,
    category VARCHAR(32) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resolved_at TIMESTAMP(6),
    closed_at TIMESTAMP(6),
    row_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_tickets_requester FOREIGN KEY (requester_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tickets_assignee FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_tickets_requester_status (requester_id, status),
    INDEX idx_tickets_assignee_status (assignee_id, status),
    INDEX idx_tickets_updated_at (updated_at)
);

CREATE TABLE ticket_comments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    message VARCHAR(10000) NOT NULL,
    internal_note BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_ticket_comments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_ticket_comments_ticket_created (ticket_id, created_at)
);

CREATE TABLE ticket_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(80) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_ticket_attachments_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_ticket_attachments_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_attachments_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id) ON DELETE RESTRICT,
    INDEX idx_ticket_attachments_ticket_uploaded (ticket_id, uploaded_at)
);
