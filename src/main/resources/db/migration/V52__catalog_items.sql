-- V52: Product/Service Catalog
-- Lightweight billing item catalog (not inventory management).
-- Stores reusable products/services with default rate/tax/unit for quick invoice/quotation creation.

CREATE TABLE public.catalog_items (
    id uuid PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES public.users(id),
    name varchar(255) NOT NULL,
    description text,
    type varchar(20) NOT NULL DEFAULT 'PRODUCT',
    default_rate double precision NOT NULL DEFAULT 0.0,
    default_tax_rate double precision NOT NULL DEFAULT 0.0,
    unit varchar(50),
    dimension varchar(100),
    kgs double precision,
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    deleted_at timestamp without time zone,
    is_deleted boolean NOT NULL DEFAULT false,
    device_id varchar(255) NOT NULL,
    version integer NOT NULL DEFAULT 1,
    created_by bigint REFERENCES public.users(id),
    updated_by bigint REFERENCES public.users(id)
);

CREATE INDEX idx_catalog_items_user_updated ON public.catalog_items(user_id, updated_at);
CREATE INDEX idx_catalog_items_user_deleted ON public.catalog_items(user_id, is_deleted);
CREATE INDEX idx_catalog_items_user_active ON public.catalog_items(user_id, is_active);