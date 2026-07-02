CREATE TABLE public.recurring_invoice_schedule (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES public.clients(id),
    user_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    billing_cycle VARCHAR(50) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE NOT NULL,
    end_date DATE,
    next_run_date TIMESTAMP NOT NULL,
    last_run_date TIMESTAMP,
    auto_charge BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_recurring_invoice_schedule_user ON public.recurring_invoice_schedule(user_id, status);
CREATE INDEX idx_recurring_invoice_schedule_run ON public.recurring_invoice_schedule(status, next_run_date) WHERE status = 'ACTIVE' AND is_deleted = FALSE;
