CREATE TABLE customer_accounts (
    id             UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id      UUID           NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id    UUID           NOT NULL UNIQUE REFERENCES customers(id) ON DELETE CASCADE,
    balance        DECIMAL(12, 2) NOT NULL DEFAULT 0,
    last_updated   TIMESTAMP      NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_accounts_tenant_id ON customer_accounts(tenant_id);
CREATE INDEX idx_customer_accounts_balance   ON customer_accounts(tenant_id, balance DESC);

CREATE TABLE customer_transactions (
    id             UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id      UUID           NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id    UUID           NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    type           VARCHAR(10)    NOT NULL,
    amount         DECIMAL(12, 2) NOT NULL,
    date           DATE           NOT NULL,
    invoice_id     UUID           REFERENCES invoices(id) ON DELETE SET NULL,
    payment_method VARCHAR(20),
    notes          TEXT,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_transactions_tenant_id ON customer_transactions(tenant_id);
CREATE INDEX idx_customer_transactions_customer  ON customer_transactions(tenant_id, customer_id);
CREATE INDEX idx_customer_transactions_date      ON customer_transactions(tenant_id, date);
CREATE INDEX idx_customer_transactions_type      ON customer_transactions(tenant_id, type);
