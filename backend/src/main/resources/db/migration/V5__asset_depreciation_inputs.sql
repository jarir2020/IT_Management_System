ALTER TABLE assets
    ADD COLUMN useful_life_months INT NULL,
    ADD COLUMN salvage_value DECIMAL(12,2) NOT NULL DEFAULT 0.00;
