-- V21 was first applied with CHAR(64), while the JPA String mapping is
-- VARCHAR(64). Convert safely; SHA-256 values are fixed-length hex strings
-- and no customer/business data is involved.
ALTER TABLE public.app_release
    ALTER COLUMN sha256 TYPE VARCHAR(64)
    USING TRIM(sha256);
