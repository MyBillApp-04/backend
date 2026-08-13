-- Quotation document settings on the shared invoice_settings table.
ALTER TABLE public.invoice_settings
    ADD COLUMN IF NOT EXISTS quotation_prefix VARCHAR(20) DEFAULT 'QT',
    ADD COLUMN IF NOT EXISTS next_quotation_number INTEGER DEFAULT 1,
    ADD COLUMN IF NOT EXISTS default_quotation_validity_days INTEGER DEFAULT 30;