-- Safe creation of core performance indexes
-- Handles overlaps with V2 and applies missing targeted query fields.

-- Clients
CREATE INDEX IF NOT EXISTS idx_clients_user_updated ON clients(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_clients_user_deleted ON clients(user_id, is_deleted);

-- Client Work
CREATE INDEX IF NOT EXISTS idx_work_user_updated ON client_work(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_work_user_deleted ON client_work(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_work_client ON client_work(client_id);
CREATE INDEX IF NOT EXISTS idx_work_invoice ON client_work(invoice_id);

-- Invoices
CREATE INDEX IF NOT EXISTS idx_invoice_user_updated ON invoice(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_invoice_user_deleted ON invoice(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_invoice_client ON invoice(client_id);

-- Invoice Items
CREATE INDEX IF NOT EXISTS idx_invoice_items_user_updated ON invoice_items(user_id, updated_at);
CREATE INDEX IF NOT EXISTS idx_invoice_items_user_deleted ON invoice_items(user_id, is_deleted);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);