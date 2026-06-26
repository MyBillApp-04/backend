-- Invoice sequences are allocated per business/user. A global unique invoice
-- number rejects valid invoices from another business using the same prefix.
DO $$
DECLARE
    global_invoice_number_constraint text;
BEGIN
    SELECT conname
    INTO global_invoice_number_constraint
    FROM pg_constraint
    WHERE conrelid = 'public.invoice'::regclass
      AND contype = 'u'
      AND pg_get_constraintdef(oid) = 'UNIQUE (invoice_number)';

    IF global_invoice_number_constraint IS NOT NULL THEN
        EXECUTE format(
            'ALTER TABLE public.invoice DROP CONSTRAINT %I',
            global_invoice_number_constraint
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'public.invoice'::regclass
          AND conname = 'uq_invoice_user_number'
    ) THEN
        ALTER TABLE public.invoice
            ADD CONSTRAINT uq_invoice_user_number UNIQUE (user_id, invoice_number);
    END IF;
END $$;
