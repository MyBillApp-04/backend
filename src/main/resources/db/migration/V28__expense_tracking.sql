CREATE TABLE public.expenses (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id),
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    category VARCHAR(100) NOT NULL,
    expense_date DATE NOT NULL,
    vendor_name VARCHAR(100),
    tax_amount NUMERIC(15, 2) DEFAULT 0.00,
    receipt_url VARCHAR(255),
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_cycle VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP,
    version INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_expenses_user_date ON public.expenses(user_id, expense_date DESC);
CREATE INDEX idx_expenses_category ON public.expenses(user_id, category);
