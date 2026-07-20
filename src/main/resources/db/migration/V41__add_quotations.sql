CREATE TABLE public.quotation (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES public.clients(id) ON DELETE CASCADE,
    quotation_number VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL, -- DRAFT, SENT, ACCEPTED, REJECTED, EXPIRED
    issue_date TIMESTAMP NOT NULL,
    valid_until_date TIMESTAMP,
    notes TEXT,
    terms_and_conditions TEXT,
    pdf_url VARCHAR(255),
    pdf_path VARCHAR(255),
    subtotal NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    discount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    gross_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    net_payable NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    device_id VARCHAR(255),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_quotation_user_number UNIQUE (user_id, quotation_number)
);

CREATE TABLE public.quotation_items (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL REFERENCES public.quotation(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    dimension VARCHAR(100),
    quantity INT NOT NULL,
    kgs DOUBLE PRECISION,
    amount NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    device_id VARCHAR(255),
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE public.quotation_financial_year_sequence (
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    financial_year VARCHAR(9) NOT NULL,
    last_sequence INT NOT NULL DEFAULT 1,
    PRIMARY KEY (user_id, financial_year)
);

-- Performance & Sync indexes
CREATE INDEX idx_quotation_user_updated ON public.quotation (user_id, updated_at DESC);
CREATE INDEX idx_quotation_user_deleted ON public.quotation (user_id, is_deleted);
CREATE INDEX idx_quotation_client ON public.quotation (client_id);
CREATE INDEX idx_quotation_items_user_updated ON public.quotation_items (user_id, updated_at DESC);
CREATE INDEX idx_quotation_items_user_deleted ON public.quotation_items (user_id, is_deleted);
CREATE INDEX idx_quotation_items_quotation ON public.quotation_items (quotation_id);

-- Constraints
ALTER TABLE public.quotation
    ADD CONSTRAINT chk_quotation_money_non_negative
    CHECK (
        subtotal >= 0
        AND discount >= 0
        AND gross_amount >= 0
        AND total_amount >= 0
        AND net_payable >= 0
    );

ALTER TABLE public.quotation_items
    ADD CONSTRAINT chk_quotation_items_quantity_positive
    CHECK (quantity > 0);

ALTER TABLE public.quotation_items
    ADD CONSTRAINT chk_quotation_items_amount_non_negative
    CHECK (amount >= 0);
