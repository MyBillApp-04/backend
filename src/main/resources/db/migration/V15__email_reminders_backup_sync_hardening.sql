CREATE INDEX IF NOT EXISTS idx_email_logs_invoice_template_created
    ON public.email_logs(invoice_id, template_type, created_at DESC);

INSERT INTO public.email_templates(template_type, subject, html_body, is_deleted)
SELECT 'DUE_REMINDER', 'Invoice {{invoiceNumber}} is due soon',
       '<p>Hello {{clientName}},</p><p>Your invoice <strong>{{invoiceNumber}}</strong> is due on {{dueDate}}. Remaining amount: {{remainingAmount}}.</p>',
       false
WHERE NOT EXISTS (
    SELECT 1 FROM public.email_templates WHERE user_id IS NULL AND template_type = 'DUE_REMINDER'
);

INSERT INTO public.email_templates(template_type, subject, html_body, is_deleted)
SELECT 'OVERDUE_REMINDER', 'Invoice {{invoiceNumber}} is overdue',
       '<p>Hello {{clientName}},</p><p>Your invoice <strong>{{invoiceNumber}}</strong> is overdue. Remaining amount: {{remainingAmount}}.</p>',
       false
WHERE NOT EXISTS (
    SELECT 1 FROM public.email_templates WHERE user_id IS NULL AND template_type = 'OVERDUE_REMINDER'
);
