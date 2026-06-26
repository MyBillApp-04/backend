ALTER TABLE public.client_ledger_entries
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

CREATE INDEX IF NOT EXISTS idx_client_ledger_created_by
    ON public.client_ledger_entries(created_by);

CREATE INDEX IF NOT EXISTS idx_client_ledger_updated_by
    ON public.client_ledger_entries(updated_by);
