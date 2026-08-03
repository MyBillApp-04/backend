-- MODEL-001 & MODEL-002 Database Migration

CREATE TABLE IF NOT EXISTS fraud_checks (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    user_id BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL,
    rules_triggered VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT,
    CONSTRAINT fk_fraud_checks_payment FOREIGN KEY (payment_id) REFERENCES payments(payment_id),
    CONSTRAINT fk_fraud_checks_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_fraud_checks_payment ON fraud_checks(payment_id);
CREATE INDEX IF NOT EXISTS idx_fraud_checks_user ON fraud_checks(user_id);

CREATE TABLE IF NOT EXISTS entity_change_history (
    id UUID PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(30) NOT NULL,
    changed_by BIGINT,
    change_details TEXT,
    timestamp TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_entity_change_history_entity ON entity_change_history(entity_name, entity_id);

ALTER TABLE client_ledger_entries ADD COLUMN IF NOT EXISTS change_log TEXT;
