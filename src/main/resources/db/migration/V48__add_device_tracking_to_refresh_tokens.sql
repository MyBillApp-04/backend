-- V48: Add device tracking columns to refresh_tokens
-- Allows tracking which device issued each refresh token for
-- multi-device session management and future "active sessions" UI.

ALTER TABLE public.refresh_tokens
    ADD COLUMN IF NOT EXISTS device_id   VARCHAR(128),
    ADD COLUMN IF NOT EXISTS device_name VARCHAR(255);

-- Index for fast device-level revocation queries
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_device_id
    ON public.refresh_tokens (device_id)
    WHERE device_id IS NOT NULL;
