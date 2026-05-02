CREATE TABLE customer_groups (
    id          UUID      NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id   UUID      NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR   NOT NULL,
    description VARCHAR,
    color       VARCHAR,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_groups_tenant_id ON customer_groups(tenant_id);

ALTER TABLE customers
    ADD COLUMN group_id UUID REFERENCES customer_groups(id) ON DELETE SET NULL;

CREATE INDEX idx_customers_group_id ON customers(group_id);
