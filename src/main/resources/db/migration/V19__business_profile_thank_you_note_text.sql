-- The API and JPA entity support a full invoice footer. Keep the database
-- column unbounded as well; varchar(255) rejects otherwise valid saves.
ALTER TABLE public.business_profile
    ALTER COLUMN thank_you_note TYPE text
    USING thank_you_note::text;
