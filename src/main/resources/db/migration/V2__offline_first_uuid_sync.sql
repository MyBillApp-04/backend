CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE SEQUENCE IF NOT EXISTS invoice_seq START 1;

ALTER TABLE clients
    ALTER COLUMN id TYPE uuid USING id::text::uuid;

ALTER TABLE client_work
    ALTER COLUMN id TYPE uuid USING id::text::uuid;

ALTER TABLE invoice
    ALTER COLUMN id TYPE uuid USING id::text::uuid;

ALTER TABLE clients
    ADD COLUMN IF NOT EXISTS created_at timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp,
    ADD COLUMN IF NOT EXISTS deleted_at timestamp,
    ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS device_id text,
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 1;

ALTER TABLE client_work
    ADD COLUMN IF NOT EXISTS created_at timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp,
    ADD COLUMN IF NOT EXISTS deleted_at timestamp,
    ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS device_id text,
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 1;

ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS invoice_date timestamp,
    ADD COLUMN IF NOT EXISTS updated_at timestamp,
    ADD COLUMN IF NOT EXISTS deleted_at timestamp,
    ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS device_id text,
    ADD COLUMN IF NOT EXISTS version integer DEFAULT 1;

UPDATE clients
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW()),
    is_deleted = COALESCE(is_deleted, false),
    version = COALESCE(version, 1);

UPDATE client_work
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW()),
    is_deleted = COALESCE(is_deleted, false),
    billed = COALESCE(billed, false),
    version = COALESCE(version, 1);

UPDATE invoice
SET invoice_date = COALESCE(invoice_date, created_date, NOW()),
    created_date = COALESCE(created_date, NOW()),
    updated_at = COALESCE(updated_at, created_date, NOW()),
    is_deleted = COALESCE(is_deleted, false),
    version = COALESCE(version, 1);

CREATE TABLE IF NOT EXISTS invoice_items (
    id uuid PRIMARY KEY,
    invoice_id uuid NOT NULL REFERENCES invoice(id),
    work_id uuid NOT NULL REFERENCES client_work(id),
    user_id bigint NOT NULL REFERENCES users(id),

    description text,
    rate double precision,
    quantity integer,
    amount double precision,

    created_at timestamp NOT NULL DEFAULT NOW(),
    updated_at timestamp NOT NULL DEFAULT NOW(),
    deleted_at timestamp,
    is_deleted boolean NOT NULL DEFAULT false,
    device_id text
);

CREATE INDEX IF NOT EXISTS idx_clients_user_updated
    ON clients(user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_clients_user_deleted
    ON clients(user_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_work_user_updated
    ON client_work(user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_work_client_user_deleted
    ON client_work(client_id, user_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_invoice_user_updated
    ON invoice(user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_invoice_client_user_deleted
    ON invoice(client_id, user_id, is_deleted);

CREATE INDEX IF NOT EXISTS idx_invoice_items_user_updated
    ON invoice_items(user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_user_deleted
    ON invoice_items(invoice_id, user_id, is_deleted);