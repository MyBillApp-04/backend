ALTER TABLE public.payments
    ADD COLUMN IF NOT EXISTS applied_to_invoice boolean NOT NULL DEFAULT false;

UPDATE public.payments
SET applied_to_invoice = true
WHERE stripe_status = 'succeeded'
  AND COALESCE(is_deleted, false) = false
  AND applied_to_invoice = false;

CREATE INDEX IF NOT EXISTS idx_payments_user_applied
    ON public.payments(user_id, applied_to_invoice)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_payments_invoice_applied
    ON public.payments(invoice_id, applied_to_invoice)
    WHERE is_deleted = false;

ALTER TABLE public.invoice_settings
    ADD COLUMN IF NOT EXISTS user_id bigint,
    ADD COLUMN IF NOT EXISTS invoice_prefix varchar(20),
    ADD COLUMN IF NOT EXISTS next_invoice_number integer,
    ADD COLUMN IF NOT EXISTS terms_and_conditions text,
    ADD COLUMN IF NOT EXISTS payment_note text,
    ADD COLUMN IF NOT EXISTS upi_id varchar(100);

CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_settings_user_unique
    ON public.invoice_settings(user_id)
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invoice_user_payment_date
    ON public.invoice(user_id, payment_date DESC)
    WHERE is_deleted = false;
