-- Flyway migration to add version column to payments table for optimistic locking.
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS version integer NOT NULL DEFAULT 1;
