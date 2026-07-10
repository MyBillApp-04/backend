ALTER TABLE public.backup_jobs
    ADD COLUMN IF NOT EXISTS sha256 VARCHAR(64);

ALTER TABLE public.backup_jobs
    DROP CONSTRAINT IF EXISTS ck_backup_jobs_sha256_hex;

ALTER TABLE public.backup_jobs
    ADD CONSTRAINT ck_backup_jobs_sha256_hex
        CHECK (sha256 IS NULL OR sha256 ~* '^[a-f0-9]{64}$');
