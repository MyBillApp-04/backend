-- V50: GST / Tax engine snapshot columns.
-- Adds invoice-level tax snapshot fields and state/GSTIN fields used for
-- intra/inter-state GST determination. Legacy (non-tax) invoices remain valid:
-- they default to NONE / 0 tax and total = gross_amount (pre-tax base).

-- Invoice: GST snapshot
ALTER TABLE public.invoice
    ADD COLUMN IF NOT EXISTS tax_rate double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS tax_type varchar(20) DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS taxable_amount double precision,
    ADD COLUMN IF NOT EXISTS tax_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS cgst_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS sgst_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS igst_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS total double precision;

-- Backfill existing invoices so the snapshot is consistent (no tax -> total = taxable base).
UPDATE public.invoice
SET tax_rate = COALESCE(tax_rate, 0.0),
    tax_type = COALESCE(tax_type, 'NONE'),
    taxable_amount = COALESCE(taxable_amount, COALESCE(gross_amount, subtotal, total_amount, 0.0)),
    tax_amount = COALESCE(tax_amount, 0.0),
    cgst_amount = COALESCE(cgst_amount, 0.0),
    sgst_amount = COALESCE(sgst_amount, 0.0),
    igst_amount = COALESCE(igst_amount, 0.0),
    total = COALESCE(total, COALESCE(gross_amount, subtotal, total_amount, 0.0));

-- Clients: state + optional GSTIN for GST type resolution
ALTER TABLE public.clients
    ADD COLUMN IF NOT EXISTS state varchar(100),
    ADD COLUMN IF NOT EXISTS gstin varchar(50);

-- Business profile: state for GST type resolution
ALTER TABLE public.business_profile
    ADD COLUMN IF NOT EXISTS state varchar(100);