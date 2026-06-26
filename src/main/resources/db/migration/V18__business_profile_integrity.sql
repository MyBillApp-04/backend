-- Each account owns exactly one business profile. The application performs an
-- upsert by user_id, so this database constraint prevents ambiguous reads and
-- concurrent duplicate inserts.
CREATE UNIQUE INDEX IF NOT EXISTS uq_business_profile_user
    ON public.business_profile (user_id);
