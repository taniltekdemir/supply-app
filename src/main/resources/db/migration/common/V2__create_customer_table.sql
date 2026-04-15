CREATE TABLE customers (
    id         UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id  UUID      NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name       VARCHAR   NOT NULL,
    phone      VARCHAR,
    address    VARCHAR,
    notes      TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customers_tenant_id ON customers(tenant_id);
