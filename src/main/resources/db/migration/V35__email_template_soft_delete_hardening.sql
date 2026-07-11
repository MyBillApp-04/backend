ALTER TABLE public.email_templates
    ADD COLUMN IF NOT EXISTS is_deleted boolean DEFAULT false;

UPDATE public.email_templates
SET is_deleted = false
WHERE is_deleted IS NULL;

ALTER TABLE public.email_templates
    ALTER COLUMN is_deleted SET DEFAULT false,
    ALTER COLUMN is_deleted SET NOT NULL;
