DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_client_work_quantity_positive') THEN
        ALTER TABLE public.client_work
            ADD CONSTRAINT chk_client_work_quantity_positive
            CHECK (quantity IS NULL OR quantity > 0) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_client_work_money_non_negative') THEN
        ALTER TABLE public.client_work
            ADD CONSTRAINT chk_client_work_money_non_negative
            CHECK (
                (rate IS NULL OR rate >= 0)
                AND (amount IS NULL OR amount >= 0)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_invoice_money_non_negative') THEN
        ALTER TABLE public.invoice
            ADD CONSTRAINT chk_invoice_money_non_negative
            CHECK (
                (subtotal IS NULL OR subtotal >= 0)
                AND (discount IS NULL OR discount >= 0)
                AND (total_amount IS NULL OR total_amount >= 0)
                AND (paid_amount IS NULL OR paid_amount >= 0)
                AND (pending_amount IS NULL OR pending_amount >= 0)
                AND (remaining_amount IS NULL OR remaining_amount >= 0)
                AND (gross_amount IS NULL OR gross_amount >= 0)
                AND (net_payable IS NULL OR net_payable >= 0)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_invoice_items_quantity_positive') THEN
        ALTER TABLE public.invoice_items
            ADD CONSTRAINT chk_invoice_items_quantity_positive
            CHECK (quantity IS NULL OR quantity > 0) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_invoice_items_money_non_negative') THEN
        ALTER TABLE public.invoice_items
            ADD CONSTRAINT chk_invoice_items_money_non_negative
            CHECK (
                (rate IS NULL OR rate >= 0)
                AND (amount IS NULL OR amount >= 0)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_expenses_money_non_negative') THEN
        ALTER TABLE public.expenses
            ADD CONSTRAINT chk_expenses_money_non_negative
            CHECK (
                amount >= 0
                AND (tax_amount IS NULL OR tax_amount >= 0)
            ) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_recurring_invoice_amount_non_negative') THEN
        ALTER TABLE public.recurring_invoice_schedule
            ADD CONSTRAINT chk_recurring_invoice_amount_non_negative
            CHECK (amount >= 0) NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_client_cascade') THEN
        ALTER TABLE public.payments
            ADD CONSTRAINT fk_payments_client_cascade
            FOREIGN KEY (client_id) REFERENCES public.clients(id)
            ON DELETE CASCADE NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_invoice_set_null') THEN
        ALTER TABLE public.payments
            ADD CONSTRAINT fk_payments_invoice_set_null
            FOREIGN KEY (invoice_id) REFERENCES public.invoice(id)
            ON DELETE SET NULL NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_payments_user_cascade') THEN
        ALTER TABLE public.payments
            ADD CONSTRAINT fk_payments_user_cascade
            FOREIGN KEY (user_id) REFERENCES public.users(id)
            ON DELETE CASCADE NOT VALID;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_recurring_invoice_schedule_user_cascade') THEN
        ALTER TABLE public.recurring_invoice_schedule
            ADD CONSTRAINT fk_recurring_invoice_schedule_user_cascade
            FOREIGN KEY (user_id) REFERENCES public.users(id)
            ON DELETE CASCADE NOT VALID;
    END IF;
END $$;
