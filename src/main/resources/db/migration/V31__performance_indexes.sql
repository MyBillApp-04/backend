CREATE INDEX IF NOT EXISTS idx_invoice_user_created ON public.invoice(user_id, created_date DESC);
CREATE INDEX IF NOT EXISTS idx_payment_invoice_created ON public.payments(invoice_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_client_user_created ON public.clients(user_id, created_at DESC);
