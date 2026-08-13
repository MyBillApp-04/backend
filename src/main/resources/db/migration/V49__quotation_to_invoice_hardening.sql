-- Migration: Quotation to Invoice Hardening
-- Adds quotation link and optional structured quotation fields to invoice items.

ALTER TABLE public.invoice
    ADD COLUMN IF NOT EXISTS quotation_id UUID REFERENCES public.quotation(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uq_invoice_user_quotation'
    ) THEN
        ALTER TABLE public.invoice
            ADD CONSTRAINT uq_invoice_user_quotation UNIQUE (user_id, quotation_id);
    END IF;
END $$;

ALTER TABLE public.invoice_items
    ADD COLUMN IF NOT EXISTS dimension VARCHAR(100),
    ADD COLUMN IF NOT EXISTS kgs DOUBLE PRECISION;

-- Allow invoice items to exist without requiring a client_work entry (e.g. for quotation-derived items)
ALTER TABLE public.invoice_items
    ALTER COLUMN work_id DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_invoice_quotation ON public.invoice (quotation_id);
