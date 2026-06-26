-- MyBill sync stability hardening migration for an EXISTING database.
-- Use this after V2..V7 if your current Supabase schema already exists.
-- It does not drop data. It aligns defaults, missing columns, indexes and FK behavior.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SEQUENCE IF NOT EXISTS invoice_seq START 1;

-- Clients: sync columns and safe defaults
ALTER TABLE public.clients
  ADD COLUMN IF NOT EXISTS created_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS deleted_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS device_id varchar(255),
  ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS version integer DEFAULT 1;

UPDATE public.clients
SET created_at = COALESCE(created_at, now()),
    updated_at = COALESCE(updated_at, created_at, now()),
    is_deleted = COALESCE(is_deleted, false),
    version = COALESCE(version, 1);

ALTER TABLE public.clients
  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN updated_at SET DEFAULT now(),
  ALTER COLUMN is_deleted SET DEFAULT false,
  ALTER COLUMN version SET DEFAULT 1,
  ALTER COLUMN updated_at SET NOT NULL,
  ALTER COLUMN is_deleted SET NOT NULL,
  ALTER COLUMN version SET NOT NULL;

-- Client work: sync columns and safe defaults
ALTER TABLE public.client_work
  ADD COLUMN IF NOT EXISTS created_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS deleted_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS device_id varchar(255),
  ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS version integer DEFAULT 1,
  ADD COLUMN IF NOT EXISTS rate double precision DEFAULT 0.0,
  ADD COLUMN IF NOT EXISTS quantity integer DEFAULT 1,
  ADD COLUMN IF NOT EXISTS billed boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS invoice_id uuid;

UPDATE public.client_work
SET created_at = COALESCE(created_at, now()),
    updated_at = COALESCE(updated_at, created_at, now()),
    is_deleted = COALESCE(is_deleted, false),
    version = COALESCE(version, 1),
    billed = COALESCE(billed, false),
    quantity = COALESCE(quantity, 1),
    rate = COALESCE(rate, amount, 0.0),
    amount = COALESCE(amount, COALESCE(rate, 0.0) * COALESCE(quantity, 1));

ALTER TABLE public.client_work
  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN updated_at SET DEFAULT now(),
  ALTER COLUMN is_deleted SET DEFAULT false,
  ALTER COLUMN version SET DEFAULT 1,
  ALTER COLUMN billed SET DEFAULT false,
  ALTER COLUMN rate SET DEFAULT 0.0,
  ALTER COLUMN quantity SET DEFAULT 1,
  ALTER COLUMN updated_at SET NOT NULL,
  ALTER COLUMN is_deleted SET NOT NULL,
  ALTER COLUMN version SET NOT NULL,
  ALTER COLUMN billed SET NOT NULL;

-- Invoice: sync columns and safe defaults
ALTER TABLE public.invoice
  ADD COLUMN IF NOT EXISTS invoice_date timestamp without time zone,
  ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS deleted_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS device_id varchar(255),
  ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS version integer DEFAULT 1,
  ADD COLUMN IF NOT EXISTS subtotal double precision DEFAULT 0.0,
  ADD COLUMN IF NOT EXISTS discount double precision DEFAULT 0.0,
  ADD COLUMN IF NOT EXISTS paid_amount double precision DEFAULT 0.0,
  ADD COLUMN IF NOT EXISTS pending_amount double precision DEFAULT 0.0,
  ADD COLUMN IF NOT EXISTS payment_status varchar(50) DEFAULT 'UNPAID',
  ADD COLUMN IF NOT EXISTS payment_mode varchar(50),
  ADD COLUMN IF NOT EXISTS due_date timestamp without time zone,
  ADD COLUMN IF NOT EXISTS payment_date timestamp without time zone,
  ADD COLUMN IF NOT EXISTS notes text,
  ADD COLUMN IF NOT EXISTS pdf_url text,
  ADD COLUMN IF NOT EXISTS pdf_path text;

UPDATE public.invoice
SET created_date = COALESCE(created_date, now()),
    invoice_date = COALESCE(invoice_date, created_date, now()),
    updated_at = COALESCE(updated_at, created_date, now()),
    is_deleted = COALESCE(is_deleted, false),
    version = COALESCE(version, 1),
    subtotal = COALESCE(subtotal, total_amount, 0.0),
    discount = COALESCE(discount, 0.0),
    total_amount = COALESCE(total_amount, subtotal, 0.0),
    paid_amount = COALESCE(paid_amount, 0.0),
    pending_amount = COALESCE(pending_amount, COALESCE(total_amount, 0.0) - COALESCE(paid_amount, 0.0)),
    payment_status = COALESCE(payment_status, 'UNPAID');

ALTER TABLE public.invoice
  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN created_date SET DEFAULT now(),
  ALTER COLUMN invoice_date SET DEFAULT now(),
  ALTER COLUMN updated_at SET DEFAULT now(),
  ALTER COLUMN is_deleted SET DEFAULT false,
  ALTER COLUMN version SET DEFAULT 1,
  ALTER COLUMN subtotal SET DEFAULT 0.0,
  ALTER COLUMN discount SET DEFAULT 0.0,
  ALTER COLUMN paid_amount SET DEFAULT 0.0,
  ALTER COLUMN pending_amount SET DEFAULT 0.0,
  ALTER COLUMN payment_status SET DEFAULT 'UNPAID',
  ALTER COLUMN updated_at SET NOT NULL,
  ALTER COLUMN is_deleted SET NOT NULL,
  ALTER COLUMN version SET NOT NULL;

-- Invoice items: V2 created this table without version/defaults in some versions
CREATE TABLE IF NOT EXISTS public.invoice_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id uuid NOT NULL REFERENCES public.invoice(id) ON DELETE CASCADE,
  work_id uuid NOT NULL REFERENCES public.client_work(id) ON DELETE CASCADE,
  user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  description varchar(255),
  rate double precision DEFAULT 0.0,
  quantity integer DEFAULT 1,
  amount double precision DEFAULT 0.0,
  created_at timestamp without time zone DEFAULT now(),
  updated_at timestamp without time zone DEFAULT now(),
  deleted_at timestamp without time zone,
  is_deleted boolean NOT NULL DEFAULT false,
  device_id varchar(255),
  version integer NOT NULL DEFAULT 1
);

ALTER TABLE public.invoice_items
  ADD COLUMN IF NOT EXISTS version integer DEFAULT 1,
  ADD COLUMN IF NOT EXISTS created_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS updated_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS deleted_at timestamp without time zone,
  ADD COLUMN IF NOT EXISTS device_id varchar(255),
  ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false;

UPDATE public.invoice_items
SET created_at = COALESCE(created_at, now()),
    updated_at = COALESCE(updated_at, created_at, now()),
    is_deleted = COALESCE(is_deleted, false),
    version = COALESCE(version, 1),
    quantity = COALESCE(quantity, 1),
    rate = COALESCE(rate, 0.0),
    amount = COALESCE(amount, COALESCE(rate, 0.0) * COALESCE(quantity, 1));

ALTER TABLE public.invoice_items
  ALTER COLUMN id SET DEFAULT gen_random_uuid(),
  ALTER COLUMN created_at SET DEFAULT now(),
  ALTER COLUMN updated_at SET DEFAULT now(),
  ALTER COLUMN is_deleted SET DEFAULT false,
  ALTER COLUMN version SET DEFAULT 1,
  ALTER COLUMN updated_at SET NOT NULL,
  ALTER COLUMN is_deleted SET NOT NULL,
  ALTER COLUMN version SET NOT NULL;

-- Invoice settings: repair V6 placeholder table
CREATE TABLE IF NOT EXISTS public.invoice_settings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamp with time zone DEFAULT now()
);

ALTER TABLE public.invoice_settings
  ADD COLUMN IF NOT EXISTS user_id bigint,
  ADD COLUMN IF NOT EXISTS invoice_prefix varchar(20) DEFAULT 'INV',
  ADD COLUMN IF NOT EXISTS next_invoice_number integer DEFAULT 1,
  ADD COLUMN IF NOT EXISTS default_due_days integer DEFAULT 7,
  ADD COLUMN IF NOT EXISTS terms_and_conditions text,
  ADD COLUMN IF NOT EXISTS payment_note text,
  ADD COLUMN IF NOT EXISTS upi_id varchar(100);

UPDATE public.invoice_settings
SET invoice_prefix = COALESCE(invoice_prefix, 'INV'),
    next_invoice_number = COALESCE(next_invoice_number, 1),
    default_due_days = COALESCE(default_due_days, 7);

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'invoice_settings_user_id_unique'
  ) THEN
    ALTER TABLE public.invoice_settings
      ADD CONSTRAINT invoice_settings_user_id_unique UNIQUE (user_id);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'fk_invoice_settings_user'
  ) THEN
    ALTER TABLE public.invoice_settings
      ADD CONSTRAINT fk_invoice_settings_user FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;
  END IF;
END $$;

-- Business profile defaults
ALTER TABLE public.business_profile
  ADD COLUMN IF NOT EXISTS terms_and_conditions text,
  ADD COLUMN IF NOT EXISTS thank_you_note text,
  ADD COLUMN IF NOT EXISTS signature_path varchar(255),
  ADD COLUMN IF NOT EXISTS invoice_prefix varchar(255) DEFAULT 'INV',
  ADD COLUMN IF NOT EXISTS next_invoice_number integer DEFAULT 1,
  ADD COLUMN IF NOT EXISTS financial_year_enabled boolean DEFAULT false;

UPDATE public.business_profile
SET created_at = COALESCE(created_at, now()),
    updated_at = COALESCE(updated_at, created_at, now()),
    invoice_prefix = COALESCE(invoice_prefix, 'INV'),
    next_invoice_number = COALESCE(next_invoice_number, 1),
    financial_year_enabled = COALESCE(financial_year_enabled, false);

ALTER TABLE public.business_profile
  ALTER COLUMN created_at SET DEFAULT now(),
  ALTER COLUMN updated_at SET DEFAULT now(),
  ALTER COLUMN invoice_prefix SET DEFAULT 'INV',
  ALTER COLUMN next_invoice_number SET DEFAULT 1,
  ALTER COLUMN financial_year_enabled SET DEFAULT false;

-- Important indexes used by sync pull queries
CREATE INDEX IF NOT EXISTS idx_clients_user_updated ON public.clients(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_clients_user_deleted ON public.clients(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_work_user_updated ON public.client_work(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_work_client_user_deleted ON public.client_work(client_id, user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_invoice_user_updated ON public.invoice(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_invoice_client_user_deleted ON public.invoice(client_id, user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_invoice_items_user_updated ON public.invoice_items(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice_user_deleted ON public.invoice_items(invoice_id, user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_business_profile_user ON public.business_profile(user_id);
