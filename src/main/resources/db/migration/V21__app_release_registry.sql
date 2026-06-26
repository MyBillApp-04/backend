-- The registry contains metadata only; customer billing data is never touched.
CREATE TABLE IF NOT EXISTS public.app_release (
    id BIGSERIAL PRIMARY KEY,
    version_code INTEGER NOT NULL UNIQUE CHECK (version_code > 0),
    version_name VARCHAR(40) NOT NULL,
    apk_url VARCHAR(2048) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    force_update BOOLEAN NOT NULL DEFAULT FALSE,
    release_notes VARCHAR(2000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_app_release_one_active
    ON public.app_release ((active)) WHERE active;
