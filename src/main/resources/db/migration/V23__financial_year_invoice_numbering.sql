-- Preserve every historical invoice number. Only invoices created after this
-- migration receive financial_year and sequence_no values.
ALTER TABLE public.invoice
    ADD COLUMN IF NOT EXISTS financial_year VARCHAR(9),
    ADD COLUMN IF NOT EXISTS sequence_no INTEGER;

-- One database row per Indian financial year; PostgreSQL UPSERT atomically
-- increments last_sequence and is safe across concurrent application nodes.
CREATE TABLE IF NOT EXISTS public.invoice_financial_year_sequence (
    financial_year VARCHAR(9) PRIMARY KEY,
    last_sequence INTEGER NOT NULL CHECK (last_sequence >= 0),
    CHECK (financial_year ~ '^[0-9]{4}-[0-9]{4}$')
);

-- Legacy rows can contain duplicate per-user invoice numbers. Partial unique
-- indexes enforce the new global standard without changing historical data.
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_new_global_number
    ON public.invoice (invoice_number)
    WHERE financial_year IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_financial_year_sequence
    ON public.invoice (financial_year, sequence_no)
    WHERE financial_year IS NOT NULL AND sequence_no IS NOT NULL;

ALTER TABLE public.invoice
    ADD CONSTRAINT ck_invoice_financial_year_sequence_pair
    CHECK (
        (financial_year IS NULL AND sequence_no IS NULL)
        OR (financial_year ~ '^[0-9]{4}-[0-9]{4}$' AND sequence_no BETWEEN 1 AND 9999)
    );
