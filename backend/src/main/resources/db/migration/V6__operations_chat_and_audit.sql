ALTER TABLE tickets
    ADD COLUMN first_responded_at TIMESTAMP(6) NULL;

CREATE TABLE ticket_chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_ticket_chat_ticket_created (ticket_id, created_at),
    CONSTRAINT fk_ticket_chat_ticket FOREIGN KEY (ticket_id) REFERENCES tickets (id) ON DELETE CASCADE,
    CONSTRAINT fk_ticket_chat_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE TABLE audit_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NULL,
    actor_email VARCHAR(254) NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(48) NOT NULL,
    entity_id BIGINT NULL,
    summary VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_audit_events_created (created_at),
    KEY ix_audit_events_entity (entity_type, entity_id)
);
