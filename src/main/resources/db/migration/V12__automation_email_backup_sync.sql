CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE public.clients
    ADD COLUMN IF NOT EXISTS email varchar(255);

CREATE TABLE IF NOT EXISTS public.email_templates (
    template_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint REFERENCES public.users(id) ON DELETE CASCADE,
    template_type varchar(80) NOT NULL,
    subject varchar(255) NOT NULL,
    html_body text NOT NULL,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    deleted_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS public.email_logs (
    email_log_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint REFERENCES public.users(id) ON DELETE SET NULL,
    recipient varchar(255),
    subject varchar(255),
    body text,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    template_type varchar(80),
    invoice_id uuid,
    attempt_count integer NOT NULL DEFAULT 0,
    sent_at timestamp without time zone,
    next_retry_at timestamp without time zone,
    error_message text,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.backup_jobs (
    backup_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    provider varchar(40) NOT NULL,
    status varchar(40) NOT NULL DEFAULT 'REQUESTED',
    location text,
    error_message text,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    completed_at timestamp without time zone
);

CREATE TABLE IF NOT EXISTS public.sync_device_state (
    sync_device_state_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    device_id varchar(255) NOT NULL,
    last_pulled_at timestamp without time zone,
    last_pushed_at timestamp without time zone,
    last_seen_at timestamp without time zone NOT NULL DEFAULT now(),
    conflict_count integer NOT NULL DEFAULT 0,
    CONSTRAINT sync_device_state_user_device_unique UNIQUE (user_id, device_id)
);

CREATE INDEX IF NOT EXISTS idx_email_templates_user_type ON public.email_templates(user_id, template_type);
CREATE INDEX IF NOT EXISTS idx_email_logs_user_status ON public.email_logs(user_id, status);
CREATE INDEX IF NOT EXISTS idx_email_logs_next_retry ON public.email_logs(next_retry_at);
CREATE INDEX IF NOT EXISTS idx_backup_jobs_user_created ON public.backup_jobs(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_sync_device_user_device ON public.sync_device_state(user_id, device_id);

INSERT INTO public.email_templates(template_type, subject, html_body)
SELECT 'INVOICE', 'Invoice {{invoiceNumber}} from MyBill',
       '<p>Hello {{clientName}},</p><p>Your invoice <strong>{{invoiceNumber}}</strong> for {{totalAmount}} is attached.</p>'
WHERE NOT EXISTS (
    SELECT 1 FROM public.email_templates WHERE user_id IS NULL AND template_type = 'INVOICE'
);

INSERT INTO public.email_templates(template_type, subject, html_body)
SELECT 'REMINDER', 'Payment reminder for invoice {{invoiceNumber}}',
       '<p>Hello {{clientName}},</p><p>This is a reminder that invoice <strong>{{invoiceNumber}}</strong> has {{remainingAmount}} remaining.</p>'
WHERE NOT EXISTS (
    SELECT 1 FROM public.email_templates WHERE user_id IS NULL AND template_type = 'REMINDER'
);

INSERT INTO public.email_templates(template_type, subject, html_body)
SELECT 'WELCOME', 'Welcome to MyBill',
       '<p>Hello {{name}},</p><p>Welcome to MyBill.</p>'
WHERE NOT EXISTS (
    SELECT 1 FROM public.email_templates WHERE user_id IS NULL AND template_type = 'WELCOME'
);
