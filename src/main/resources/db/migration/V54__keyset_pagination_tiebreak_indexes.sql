-- Keyset-pagination tie-breaker indexes.
--
-- The sync pull queries page each entity with:
--   WHERE user_id = ? AND (updated_at > :t OR (updated_at = :t AND id > :i))
--   ORDER BY updated_at ASC, id ASC
-- The existing (user_id, updated_at) indexes cannot serve the equal-timestamp
-- tie-break branch as a pure index scan. These composite indexes append `id` so
-- Postgres can satisfy the ordering and the tie-break without a sort/filter.
--
-- Additive and idempotent; safe to run on existing data.

CREATE INDEX IF NOT EXISTS idx_clients_user_updated_id
    ON public.clients (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_client_work_user_updated_id
    ON public.client_work (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_invoice_user_updated_id
    ON public.invoice (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_invoice_items_user_updated_id
    ON public.invoice_items (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_client_ledger_user_updated_id
    ON public.client_ledger_entries (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_quotation_user_updated_id
    ON public.quotation (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_quotation_items_user_updated_id
    ON public.quotation_items (user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_catalog_items_user_updated_id
    ON public.catalog_items (user_id, updated_at, id);
