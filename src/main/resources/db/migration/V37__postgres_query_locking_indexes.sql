-- Query-plan and scheduler-locking hardening for PostgreSQL.
-- All objects are additive/idempotent so existing data and API behavior remain unchanged.

-- Async retry worker: status/next-run filtering plus created_at ordering.
CREATE INDEX IF NOT EXISTS idx_async_jobs_executable_created
    ON public.async_jobs(status, next_run_at, created_at, job_id)
    WHERE status IN ('PENDING', 'FAILED');

CREATE INDEX IF NOT EXISTS idx_async_jobs_status_created_desc
    ON public.async_jobs(status, created_at DESC, job_id);

-- Recurring invoice scheduler: due-row scan ordered by next run and stable id.
CREATE INDEX IF NOT EXISTS idx_recurring_invoice_due_ids
    ON public.recurring_invoice_schedule(status, next_run_date, id)
    WHERE is_deleted = false;

-- Customer notification retry/reminder checks.
CREATE INDEX IF NOT EXISTS idx_cust_notification_retry
    ON public.customer_notification_logs(status, retry_count, created_time, notification_id)
    WHERE status = 'FAILED';

CREATE INDEX IF NOT EXISTS idx_cust_notification_invoice_type_status_created
    ON public.customer_notification_logs(invoice_id, notification_type, status, created_time DESC)
    WHERE status = 'SENT';

CREATE INDEX IF NOT EXISTS idx_cust_notification_user_created_desc
    ON public.customer_notification_logs(user_id, created_time DESC, notification_id);

-- Backup history.
CREATE INDEX IF NOT EXISTS idx_backup_jobs_user_created_desc
    ON public.backup_jobs(user_id, created_at DESC, backup_id);

-- Client ledger page: includes user_id because every API lookup is user-scoped.
CREATE INDEX IF NOT EXISTS idx_client_ledger_client_user_tx_desc
    ON public.client_ledger_entries(client_id, user_id, transaction_date DESC, id)
    WHERE is_deleted = false;

-- Expense reports. Existing indexes are general-purpose; these match active-record reads.
CREATE INDEX IF NOT EXISTS idx_expenses_active_user_date
    ON public.expenses(user_id, expense_date DESC, id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_expenses_active_user_category
    ON public.expenses(user_id, category, expense_date DESC)
    WHERE is_deleted = false;

-- Reporting code uses EXTRACT(YEAR/MONTH). These expression indexes support that
-- without changing report output or date boundary semantics.
CREATE INDEX IF NOT EXISTS idx_invoice_active_user_created_year_month
    ON public.invoice(user_id, (EXTRACT(YEAR FROM created_date)), (EXTRACT(MONTH FROM created_date)))
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_expenses_active_user_year_month
    ON public.expenses(user_id, (EXTRACT(YEAR FROM expense_date)), (EXTRACT(MONTH FROM expense_date)))
    WHERE is_deleted = false;

-- Advanced reports filter on invoice_date and status separately from created_date.
CREATE INDEX IF NOT EXISTS idx_invoice_active_user_invoice_date
    ON public.invoice(user_id, invoice_date, id)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_invoice_active_user_status_due_date
    ON public.invoice(user_id, payment_status, due_date, id)
    WHERE is_deleted = false;

-- Async jobs may reference invoices; keep delete behavior non-destructive.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_async_jobs_invoice_set_null'
    ) THEN
        ALTER TABLE public.async_jobs
            ADD CONSTRAINT fk_async_jobs_invoice_set_null
            FOREIGN KEY (invoice_id) REFERENCES public.invoice(id)
            ON DELETE SET NULL NOT VALID;
    END IF;
END $$;
