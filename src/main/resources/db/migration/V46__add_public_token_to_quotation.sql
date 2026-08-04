-- V46: Store the raw public token on quotation so the sync endpoint can echo it
-- back to clients. The hash is kept for security lookups; the raw token is only
-- needed for link reconstruction during sync pull.
ALTER TABLE quotation ADD COLUMN IF NOT EXISTS public_token VARCHAR(43);

-- Backfill: existing rows already have a hash but not the raw token.
-- They will receive a proper token the next time the client calls
-- POST /api/quotations/{id}/public-link (which already regenerates).
-- Index supports fast lookup during sync serialization (optional but cheap).
CREATE INDEX IF NOT EXISTS idx_quotation_public_token ON quotation (public_token)
    WHERE public_token IS NOT NULL;
