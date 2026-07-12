-- V36__rename_role_client_to_owner.sql
--
-- Renames the 'CLIENT' role value to 'OWNER' in the users table.
--
-- Context: The Role enum used 'CLIENT' to represent business owners
-- (the primary users of the Owner Portal). This was incorrect naming.
-- The 'OWNER' value now correctly identifies business owners.
-- The 'CLIENT' value is reserved for the future Client Portal (v2).
--
-- This migration is safe and idempotent for a single-instance deployment:
--   - The existing role check constraint is widened before data is updated.
--   - Existing owner accounts continue to work after login (new JWT carries OWNER role).
--   - Any active JWTs issued before this migration carry ROLE_USER authority
--     (unchanged), so in-flight sessions are not invalidated.

ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_role_check;

ALTER TABLE users
    ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'OWNER', 'CLIENT'));

UPDATE users
SET role = 'OWNER'
WHERE role = 'CLIENT';
