CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- New domain tables
CREATE TABLE IF NOT EXISTS public.payments (
    payment_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id uuid NOT NULL REFERENCES public.clients(id),
    invoice_id uuid REFERENCES public.invoice(id),
    user_id bigint NOT NULL REFERENCES public.users(id),
    amount double precision NOT NULL,
    payment_mode varchar(50),
    stripe_payment_intent_id varchar(255),
    stripe_charge_id varchar(255),
    stripe_refund_id varchar(255),
    stripe_status varchar(100),
    refunded_amount double precision DEFAULT 0.0,
    date timestamp without time zone NOT NULL DEFAULT now(),
    notes text,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint
);

CREATE TABLE IF NOT EXISTS public.activity_logs (
    activity_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL REFERENCES public.users(id),
    action varchar(255) NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamp without time zone,
    created_by bigint,
    updated_by bigint
);

CREATE TABLE IF NOT EXISTS public.notifications (
    notification_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint REFERENCES public.users(id),
    title varchar(255) NOT NULL,
    message text NOT NULL,
    is_read boolean NOT NULL DEFAULT false,
    is_deleted boolean NOT NULL DEFAULT false,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint
);

-- Audit ownership columns on existing entities
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

ALTER TABLE public.clients
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

ALTER TABLE public.client_work
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

ALTER TABLE public.invoice
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint,
    ADD COLUMN IF NOT EXISTS remaining_amount double precision DEFAULT 0.0;

UPDATE public.invoice
SET remaining_amount = COALESCE(remaining_amount, pending_amount, COALESCE(total_amount, 0.0) - COALESCE(paid_amount, 0.0));

ALTER TABLE public.invoice_items
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

ALTER TABLE public.business_profile
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

ALTER TABLE public.invoice_settings
    ADD COLUMN IF NOT EXISTS created_at timestamp without time zone DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by bigint,
    ADD COLUMN IF NOT EXISTS updated_by bigint;

UPDATE public.invoice_settings
SET created_at = COALESCE(created_at, now()),
    updated_at = COALESCE(updated_at, created_at, now());

-- Core indexes for the new tables
CREATE INDEX IF NOT EXISTS idx_payments_client ON public.payments(client_id);
CREATE INDEX IF NOT EXISTS idx_payments_invoice ON public.payments(invoice_id);
CREATE INDEX IF NOT EXISTS idx_payments_stripe_payment_intent ON public.payments(stripe_payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payments_user_date ON public.payments(user_id, date);
CREATE INDEX IF NOT EXISTS idx_payments_user_deleted ON public.payments(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_activity_logs_user_created ON public.activity_logs(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_activity_logs_action ON public.activity_logs(action);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON public.notifications(user_id, is_read);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON public.notifications(user_id, created_at);

-- Invoice query and dashboard search indexes
CREATE INDEX IF NOT EXISTS idx_invoice_user_created ON public.invoice(user_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_user_status ON public.invoice(user_id, payment_status);
CREATE INDEX IF NOT EXISTS idx_invoice_user_client_created ON public.invoice(user_id, client_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_user_deleted_created ON public.invoice(user_id, is_deleted, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoice_number_lower ON public.invoice(LOWER(invoice_number));
CREATE INDEX IF NOT EXISTS idx_clients_name_lower ON public.clients(LOWER(name));

CREATE INDEX IF NOT EXISTS idx_invoice_full_text_search
ON public.invoice
USING gin (
    to_tsvector(
        'simple',
        COALESCE(invoice_number, '') || ' ' || COALESCE(notes, '')
    )
);

CREATE INDEX IF NOT EXISTS idx_clients_full_text_search
ON public.clients
USING gin (
    to_tsvector(
        'simple',
        COALESCE(name, '') || ' ' || COALESCE(phone, '')
    )
);
