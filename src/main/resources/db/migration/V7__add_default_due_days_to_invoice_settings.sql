DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name='invoice_settings' AND column_name='default_due_days'
    ) THEN
        ALTER TABLE invoice_settings ADD COLUMN default_due_days INT; -- (Or use your exact column data type here)
    END IF;
END $$;