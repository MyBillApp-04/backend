-- Drop the old global uniqueness constraints and indexes
DROP INDEX IF EXISTS public.uq_invoice_new_global_number;
DROP INDEX IF EXISTS public.uq_invoice_financial_year_sequence;

-- Drop the old table that lacks user isolation
DROP TABLE IF EXISTS public.invoice_financial_year_sequence;

-- Create the new user-isolated sequence table
CREATE TABLE public.invoice_financial_year_sequence (
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    financial_year VARCHAR(9) NOT NULL,
    last_sequence INTEGER NOT NULL CHECK (last_sequence >= 0),
    PRIMARY KEY (user_id, financial_year),
    CHECK (financial_year ~ '^[0-9]{4}-[0-9]{4}$')
);

-- Re-create indices to isolate invoice number and sequence per user/business
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_user_fy_sequence
    ON public.invoice (user_id, financial_year, sequence_no)
    WHERE financial_year IS NOT NULL AND sequence_no IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_invoice_user_number_fy
    ON public.invoice (user_id, invoice_number)
    WHERE financial_year IS NOT NULL;
