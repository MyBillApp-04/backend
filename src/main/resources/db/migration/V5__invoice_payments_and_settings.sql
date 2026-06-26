-- Add Business Profile Invoice Settings
ALTER TABLE business_profile
    ADD COLUMN IF NOT EXISTS invoice_prefix varchar(50) DEFAULT 'INV',
    ADD COLUMN IF NOT EXISTS next_invoice_number integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS financial_year_enabled boolean DEFAULT false;

-- Add Invoice Payment Tracking & Math Fields
ALTER TABLE invoice
    ADD COLUMN IF NOT EXISTS subtotal double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS discount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS paid_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS pending_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS payment_status varchar(50) DEFAULT 'UNPAID',
    ADD COLUMN IF NOT EXISTS payment_mode varchar(50),
    ADD COLUMN IF NOT EXISTS due_date timestamp,
    ADD COLUMN IF NOT EXISTS payment_date timestamp,
    ADD COLUMN IF NOT EXISTS notes text,
    ADD COLUMN IF NOT EXISTS pdf_url text,
    ADD COLUMN IF NOT EXISTS pdf_path text;

-- Update existing invoices to have valid default states
UPDATE invoice
SET pending_amount = total_amount,
    subtotal = total_amount
WHERE pending_amount IS NULL OR pending_amount = 0.0;

-- Update null statuses
UPDATE invoice
SET payment_status = 'UNPAID'
WHERE payment_status IS NULL;