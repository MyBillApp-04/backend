CREATE TABLE IF NOT EXISTS public.async_jobs (
    job_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type varchar(80) NOT NULL,
    payload text NOT NULL,
    status varchar(30) NOT NULL DEFAULT 'PENDING',
    attempt_count integer NOT NULL DEFAULT 0,
    max_attempts integer NOT NULL DEFAULT 5,
    next_run_at timestamp without time zone NOT NULL DEFAULT now(),
    last_error text,
    user_id bigint REFERENCES public.users(id) ON DELETE SET NULL,
    invoice_id uuid,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_async_jobs_status_next_run ON public.async_jobs(status, next_run_at);
CREATE INDEX IF NOT EXISTS idx_async_jobs_user_invoice ON public.async_jobs(user_id, invoice_id);
