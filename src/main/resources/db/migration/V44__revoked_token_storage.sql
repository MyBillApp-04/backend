CREATE TABLE revoked_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_revoked_tokens_hash_expiry ON revoked_tokens (token_hash, expires_at);
