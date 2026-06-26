-- Optional sync-status indexes.
-- Current schema does not define sync_status, so these are no-ops unless that
-- column exists in a deployed database or is added by a future migration.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'clients'
          AND column_name = 'sync_status'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_clients_sync_status ON public.clients(sync_status);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'client_work'
          AND column_name = 'sync_status'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_work_sync_status ON public.client_work(sync_status);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'invoice'
          AND column_name = 'sync_status'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_invoice_sync_status ON public.invoice(sync_status);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'invoice_items'
          AND column_name = 'sync_status'
    ) THEN
        CREATE INDEX IF NOT EXISTS idx_invoice_items_sync_status ON public.invoice_items(sync_status);
    END IF;
END $$;
