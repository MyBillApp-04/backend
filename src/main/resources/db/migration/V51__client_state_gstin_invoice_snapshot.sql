-- V51: Customer GST snapshot on invoice.
-- Persists the buyer's state + GSTIN at invoice-creation time so a tax
-- invoice remains compliant (buyer GSTIN + state required on GST invoices)
-- even if the customer is edited after the invoice is issued.

ALTER TABLE public.invoice
    ADD COLUMN IF NOT EXISTS client_state varchar(100),
    ADD COLUMN IF NOT EXISTS client_gstin varchar(50);

-- Backfill existing invoices from their current linked customer where known.
UPDATE public.invoice i
SET client_state = COALESCE(i.client_state, c.state),
    client_gstin = COALESCE(i.client_gstin, c.gstin)
FROM public.clients c
WHERE i.client_id = c.id
  AND (i.client_state IS NULL OR i.client_gstin IS NULL);