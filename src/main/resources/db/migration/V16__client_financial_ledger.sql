ALTER TABLE public.invoice
    ADD COLUMN IF NOT EXISTS gross_amount double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS advance_applied double precision DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS net_payable double precision DEFAULT 0.0;

UPDATE public.invoice
SET gross_amount = COALESCE(gross_amount, subtotal, total_amount, 0.0),
    advance_applied = COALESCE(advance_applied, 0.0),
    net_payable = COALESCE(net_payable, total_amount, 0.0),
    total_amount = COALESCE(total_amount, net_payable, 0.0),
    pending_amount = COALESCE(pending_amount, COALESCE(total_amount, 0.0) - COALESCE(paid_amount, 0.0));

ALTER TABLE public.invoice
    ALTER COLUMN gross_amount SET DEFAULT 0.0,
    ALTER COLUMN advance_applied SET DEFAULT 0.0,
    ALTER COLUMN net_payable SET DEFAULT 0.0;

CREATE TABLE IF NOT EXISTS public.client_ledger_entries (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
    invoice_id uuid REFERENCES public.invoice(id) ON DELETE SET NULL,
    payment_id uuid REFERENCES public.payments(payment_id) ON DELETE SET NULL,
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    type varchar(50) NOT NULL,
    direction varchar(20) NOT NULL,
    amount double precision NOT NULL DEFAULT 0.0,
    balance_after double precision NOT NULL DEFAULT 0.0,
    notes text,
    transaction_date timestamp without time zone NOT NULL DEFAULT now(),
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    deleted_at timestamp without time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    device_id varchar(255),
    version integer NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_client_ledger_user_updated
    ON public.client_ledger_entries(user_id, updated_at);

CREATE INDEX IF NOT EXISTS idx_client_ledger_client_date
    ON public.client_ledger_entries(client_id, transaction_date DESC)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_invoice_client_status_due
    ON public.invoice(client_id, payment_status, due_date)
    WHERE is_deleted = false;

CREATE INDEX IF NOT EXISTS idx_invoice_items_work_invoice
    ON public.invoice_items(work_id, invoice_id)
    WHERE is_deleted = false;

INSERT INTO public.client_ledger_entries (
    id, client_id, invoice_id, user_id, type, direction, amount, balance_after,
    notes, transaction_date, created_at, updated_at, is_deleted, device_id, version
)
SELECT gen_random_uuid(), i.client_id, i.id, i.user_id, 'INVOICE_CREATED', 'DEBIT',
       COALESCE(i.gross_amount, i.subtotal, i.total_amount, 0.0), 0.0,
       'Backfilled invoice creation for ' || COALESCE(i.invoice_number, i.id::text),
       COALESCE(i.invoice_date, i.created_date, now()),
       COALESCE(i.created_date, now()), now(), false, i.device_id, 1
FROM public.invoice i
WHERE COALESCE(i.is_deleted, false) = false
  AND NOT EXISTS (
      SELECT 1 FROM public.client_ledger_entries le
      WHERE le.invoice_id = i.id
        AND le.type = 'INVOICE_CREATED'
        AND COALESCE(le.is_deleted, false) = false
  );

INSERT INTO public.client_ledger_entries (
    id, client_id, invoice_id, user_id, type, direction, amount, balance_after,
    notes, transaction_date, created_at, updated_at, is_deleted, device_id, version
)
SELECT gen_random_uuid(), i.client_id, i.id, i.user_id, 'ADVANCE_APPLIED', 'CREDIT',
       COALESCE(i.advance_applied, 0.0), 0.0,
       'Backfilled advance applied for ' || COALESCE(i.invoice_number, i.id::text),
       COALESCE(i.invoice_date, i.created_date, now()),
       COALESCE(i.created_date, now()), now(), false, i.device_id, 1
FROM public.invoice i
WHERE COALESCE(i.is_deleted, false) = false
  AND COALESCE(i.advance_applied, 0.0) > 0
  AND NOT EXISTS (
      SELECT 1 FROM public.client_ledger_entries le
      WHERE le.invoice_id = i.id
        AND le.type = 'ADVANCE_APPLIED'
        AND COALESCE(le.is_deleted, false) = false
  );

INSERT INTO public.client_ledger_entries (
    id, client_id, invoice_id, user_id, type, direction, amount, balance_after,
    notes, transaction_date, created_at, updated_at, is_deleted, device_id, version
)
SELECT gen_random_uuid(), i.client_id, i.id, i.user_id, 'PAYMENT_RECEIVED', 'CREDIT',
       COALESCE(i.paid_amount, 0.0), 0.0,
       'Backfilled payment for ' || COALESCE(i.invoice_number, i.id::text),
       COALESCE(i.payment_date, i.updated_at, now()),
       COALESCE(i.updated_at, now()), now(), false, i.device_id, 1
FROM public.invoice i
WHERE COALESCE(i.is_deleted, false) = false
  AND COALESCE(i.paid_amount, 0.0) > 0
  AND NOT EXISTS (
      SELECT 1 FROM public.client_ledger_entries le
      WHERE le.invoice_id = i.id
        AND le.type = 'PAYMENT_RECEIVED'
        AND COALESCE(le.is_deleted, false) = false
  );
