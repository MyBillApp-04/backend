CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Database-side audit timestamp safety net. JPA still owns timestamps in normal app flows,
-- while this protects direct SQL maintenance and future batch jobs.
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
DECLARE
    target_table text;
BEGIN
    FOREACH target_table IN ARRAY ARRAY[
        'users',
        'clients',
        'client_work',
        'invoice',
        'invoice_items',
        'business_profile',
        'invoice_settings',
        'payments',
        'activity_logs',
        'notifications'
    ]
    LOOP
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = target_table
              AND column_name = 'updated_at'
        ) THEN
            EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_set_updated_at ON public.%I', target_table, target_table);
            EXECUTE format(
                'CREATE TRIGGER trg_%I_set_updated_at
                 BEFORE UPDATE ON public.%I
                 FOR EACH ROW
                 EXECUTE FUNCTION public.set_updated_at()',
                target_table,
                target_table
            );
        END IF;
    END LOOP;
END $$;

-- Soft-delete friendly partial indexes keep common active-record reads tight.
CREATE INDEX IF NOT EXISTS idx_clients_active_user_name
    ON public.clients(user_id, lower(name))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_work_active_user_date
    ON public.client_work(user_id, date DESC, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_work_active_client_unbilled
    ON public.client_work(client_id, user_id, billed, date ASC, created_at ASC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_invoice_active_user_created
    ON public.invoice(user_id, created_date DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_invoice_active_user_status_created
    ON public.invoice(user_id, payment_status, created_date DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_invoice_items_active_invoice_user
    ON public.invoice_items(invoice_id, user_id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payments_active_user_date
    ON public.payments(user_id, date DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_activity_logs_active_user_created
    ON public.activity_logs(user_id, created_at DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_notifications_active_user_read_created
    ON public.notifications(user_id, is_read, created_at DESC)
    WHERE is_deleted = false;

-- Trigram indexes accelerate the existing case-insensitive contains searches.
CREATE INDEX IF NOT EXISTS idx_clients_name_trgm
    ON public.clients USING gin (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_clients_phone_trgm
    ON public.clients USING gin (lower(phone) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_clients_email_trgm
    ON public.clients USING gin (lower(email) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_work_description_trgm
    ON public.client_work USING gin (lower(description) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_invoice_number_trgm
    ON public.invoice USING gin (lower(invoice_number) gin_trgm_ops);

-- Full-text indexes for broader search screens.
CREATE INDEX IF NOT EXISTS idx_work_full_text_search
    ON public.client_work
    USING gin (
        to_tsvector('simple', COALESCE(description, ''))
    );

CREATE INDEX IF NOT EXISTS idx_notifications_full_text_search
    ON public.notifications
    USING gin (
        to_tsvector('simple', COALESCE(title, '') || ' ' || COALESCE(message, ''))
    );

-- Guardrails for payment rows.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_payments_amount_non_negative'
    ) THEN
        ALTER TABLE public.payments
            ADD CONSTRAINT chk_payments_amount_non_negative
            CHECK (amount >= 0) NOT VALID;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_payments_refunded_amount_non_negative'
    ) THEN
        ALTER TABLE public.payments
            ADD CONSTRAINT chk_payments_refunded_amount_non_negative
            CHECK (refunded_amount IS NULL OR refunded_amount >= 0) NOT VALID;
    END IF;
END $$;
