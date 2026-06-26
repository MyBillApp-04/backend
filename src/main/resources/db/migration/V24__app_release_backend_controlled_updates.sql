-- Expands only the app_release registry. Customer, invoice, payment, ledger,
-- backup, and settings tables are intentionally untouched.
ALTER TABLE public.app_release
    ADD COLUMN IF NOT EXISTS minimum_supported_version_code INTEGER,
    ADD COLUMN IF NOT EXISTS apk_url_primary VARCHAR(2048),
    ADD COLUMN IF NOT EXISTS apk_url_fallback VARCHAR(2048),
    ADD COLUMN IF NOT EXISTS update_type VARCHAR(10),
    ADD COLUMN IF NOT EXISTS remind_after_days INTEGER,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE;

UPDATE public.app_release
SET apk_url_primary = COALESCE(apk_url_primary, apk_url),
    minimum_supported_version_code = COALESCE(
        minimum_supported_version_code,
        CASE WHEN force_update THEN version_code ELSE 1 END
    ),
    update_type = COALESCE(update_type, CASE WHEN force_update THEN 'FORCE' ELSE 'SOFT' END),
    remind_after_days = COALESCE(remind_after_days, CASE WHEN force_update THEN 0 ELSE 3 END),
    created_at = COALESCE(created_at, published_at, CURRENT_TIMESTAMP),
    updated_at = COALESCE(updated_at, published_at, CURRENT_TIMESTAMP);

ALTER TABLE public.app_release
    ALTER COLUMN minimum_supported_version_code SET NOT NULL,
    ALTER COLUMN apk_url_primary SET NOT NULL,
    ALTER COLUMN update_type SET NOT NULL,
    ALTER COLUMN remind_after_days SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE public.app_release
    ADD CONSTRAINT ck_app_release_minimum_supported_positive
        CHECK (minimum_supported_version_code > 0),
    ADD CONSTRAINT ck_app_release_primary_https
        CHECK (apk_url_primary ~ '^https://.+'),
    ADD CONSTRAINT ck_app_release_fallback_https
        CHECK (apk_url_fallback IS NULL OR apk_url_fallback ~ '^https://.+'),
    ADD CONSTRAINT ck_app_release_sha256_hex
        CHECK (sha256 ~* '^[a-f0-9]{64}$'),
    ADD CONSTRAINT ck_app_release_update_type
        CHECK (update_type IN ('NONE', 'SOFT', 'FORCE')),
    ADD CONSTRAINT ck_app_release_remind_after_days
        CHECK (remind_after_days >= 0),
    ADD CONSTRAINT ck_app_release_release_notes_not_blank
        CHECK (length(trim(release_notes)) > 0);

CREATE OR REPLACE FUNCTION public.touch_app_release_updated_at()
RETURNS trigger AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_app_release_touch_updated_at ON public.app_release;
CREATE TRIGGER trg_app_release_touch_updated_at
    BEFORE UPDATE ON public.app_release
    FOR EACH ROW
    EXECUTE FUNCTION public.touch_app_release_updated_at();
