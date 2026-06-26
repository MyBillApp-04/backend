-- Add itemized billing columns to existing client_work table
ALTER TABLE client_work
    ADD COLUMN IF NOT EXISTS rate double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS quantity integer DEFAULT 1,
    ADD COLUMN IF NOT EXISTS billed boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS invoice_id uuid;

-- Update existing records to have a valid rate/quantity based on their current amount
UPDATE client_work
SET rate = amount,
    quantity = 1
WHERE rate IS NULL OR rate = 0.0;