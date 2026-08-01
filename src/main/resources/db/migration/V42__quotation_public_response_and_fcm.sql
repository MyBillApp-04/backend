-- Flyway Migration V42: Add Quotation Public Response Links & FCM Device Tokens

-- 1. Extend public.quotation table for response link & status tracking
ALTER TABLE public.quotation
    ADD COLUMN IF NOT EXISTS public_token_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS token_created_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS token_expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS token_revoked_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS client_response_status VARCHAR(30) DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS responded_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS discussion_message TEXT;

CREATE INDEX IF NOT EXISTS idx_quotation_public_token_hash ON public.quotation (public_token_hash);

-- 2. Create table for quotation response audit events
CREATE TABLE IF NOT EXISTS public.quotation_response_event (
    id UUID PRIMARY KEY,
    quotation_id UUID NOT NULL REFERENCES public.quotation(id) ON DELETE CASCADE,
    action VARCHAR(30) NOT NULL,
    responded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    discussion_message TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_quotation_response_event_quotation ON public.quotation_response_event (quotation_id, responded_at DESC);

-- 3. Create table for FCM user device registration tokens
CREATE TABLE IF NOT EXISTS public.user_device_token (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    fcm_token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL DEFAULT 'UNKNOWN',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_device_token UNIQUE (user_id, fcm_token)
);

CREATE INDEX IF NOT EXISTS idx_user_device_token_user ON public.user_device_token (user_id);
