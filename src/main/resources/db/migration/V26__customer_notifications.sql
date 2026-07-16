-- Migration to add customer notification settings, templates, and history logs
CREATE TABLE IF NOT EXISTS public.customer_notification_settings (
    id uuid PRIMARY KEY,
    user_id bigint UNIQUE NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    enable_whatsapp boolean NOT NULL DEFAULT true,
    enable_invoice_generated boolean NOT NULL DEFAULT true,
    enable_payment_received boolean NOT NULL DEFAULT true,
    enable_partial_payment boolean NOT NULL DEFAULT true,
    enable_invoice_updated boolean NOT NULL DEFAULT true,
    enable_advance_balance boolean NOT NULL DEFAULT true,
    enable_payment_reminder boolean NOT NULL DEFAULT true,
    reminder_frequency_days integer NOT NULL DEFAULT 7,
    enable_powered_by_mybill boolean NOT NULL DEFAULT true,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    created_by bigint,
    updated_by bigint
);

CREATE TABLE IF NOT EXISTS public.customer_notification_templates (
    id uuid PRIMARY KEY,
    user_id bigint REFERENCES public.users(id) ON DELETE CASCADE,
    template_type varchar(50) NOT NULL, /* INVOICE_GENERATED, PAYMENT_RECEIVED, PARTIAL_PAYMENT, INVOICE_UPDATED, ADVANCE_BALANCE, PAYMENT_REMINDER */
    channel varchar(50) NOT NULL, /* WHATSAPP, EMAIL, SMS, PUSH */
    subject varchar(255),
    message_body text NOT NULL,
    is_deleted boolean NOT NULL DEFAULT false,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    deleted_at timestamp without time zone,
    CONSTRAINT uq_user_template_type_channel UNIQUE(user_id, template_type, channel)
);

CREATE TABLE IF NOT EXISTS public.customer_notification_logs (
    notification_id uuid PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    customer_id uuid NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
    invoice_id uuid REFERENCES public.invoice(id) ON DELETE SET NULL,
    phone_number varchar(50) NOT NULL,
    notification_type varchar(50) NOT NULL,
    channel varchar(50) NOT NULL,
    status varchar(50) NOT NULL, /* PENDING, SENT, FAILED, RETRYING */
    created_time timestamp without time zone NOT NULL DEFAULT now(),
    sent_time timestamp without time zone,
    failure_reason text,
    retry_count integer NOT NULL DEFAULT 0,
    provider_response text,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

-- Indexing for notification logs
CREATE INDEX IF NOT EXISTS idx_cust_notification_logs_user_status ON public.customer_notification_logs(user_id, status);
CREATE INDEX IF NOT EXISTS idx_cust_notification_logs_created ON public.customer_notification_logs(created_time);

-- Prepopulate default system-wide templates (where user_id is null)
INSERT INTO public.customer_notification_templates (id, user_id, template_type, channel, subject, message_body)
VALUES
    (gen_random_uuid(), NULL, 'INVOICE_GENERATED', 'WHATSAPP', NULL, 'Hello {{customerName}},

Your invoice has been generated.

Business:
{{businessName}}

Invoice:
{{invoiceNumber}}

Amount:
₹{{amount}}

Status:
{{paymentStatus}}

Thank you for choosing {{businessName}}.'),

    (gen_random_uuid(), NULL, 'PAYMENT_RECEIVED', 'WHATSAPP', NULL, 'Hello {{customerName}},

Your payment has been received in full.

Invoice:
{{invoiceNumber}}

Paid Amount:
₹{{receivedAmount}}

Remaining Amount:
₹0

Status:
Paid

Thank you for choosing {{businessName}}.'),

    (gen_random_uuid(), NULL, 'PARTIAL_PAYMENT', 'WHATSAPP', NULL, 'Hello {{customerName}},

A partial payment has been received.

Invoice:
{{invoiceNumber}}

Amount Received:
₹{{receivedAmount}}

Remaining Amount:
₹{{remainingAmount}}

Outstanding Balance:
₹{{remainingAmount}}

Thank you for choosing {{businessName}}.'),

    (gen_random_uuid(), NULL, 'INVOICE_UPDATED', 'WHATSAPP', NULL, 'Hello {{customerName}},

Your invoice has been updated.

Please refer to the latest invoice details.

Business:
{{businessName}}

Invoice:
{{invoiceNumber}}

Amount:
₹{{amount}}'),

    (gen_random_uuid(), NULL, 'ADVANCE_BALANCE', 'WHATSAPP', NULL, 'Hello {{customerName}},

An adjustment has been made. You have an advance balance of ₹{{advanceAmount}} available.

This advance will automatically be adjusted in your future invoices.'),

    (gen_random_uuid(), NULL, 'PAYMENT_REMINDER', 'WHATSAPP', NULL, 'Hello {{customerName}},

This is a friendly reminder that your invoice remains unpaid.

Business:
{{businessName}}

Invoice:
{{invoiceNumber}}

Pending Amount:
₹{{amount}}

Please arrange payment at your earliest convenience. Thank you!');
