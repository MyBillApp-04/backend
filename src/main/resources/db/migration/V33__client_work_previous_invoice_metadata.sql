ALTER TABLE public.client_work
    ADD COLUMN IF NOT EXISTS previous_invoice_number VARCHAR(255);

ALTER TABLE public.client_work
    ADD COLUMN IF NOT EXISTS last_billed_date TIMESTAMP;
