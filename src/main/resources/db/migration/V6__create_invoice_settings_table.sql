CREATE TABLE IF NOT EXISTS invoice_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    -- add your other columns matching your Java Entity fields here
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);