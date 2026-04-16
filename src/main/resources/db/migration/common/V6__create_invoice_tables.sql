CREATE TABLE invoices (
    id           UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id    UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id  UUID        NOT NULL REFERENCES customers(id),
    invoice_date DATE        NOT NULL,
    status       VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_at   TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoices_tenant_id     ON invoices(tenant_id);
CREATE INDEX idx_invoices_tenant_date   ON invoices(tenant_id, invoice_date);
CREATE INDEX idx_invoices_tenant_status ON invoices(tenant_id, status);

CREATE TABLE invoice_items (
    id          UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id   UUID           NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id  UUID           NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id  UUID           NOT NULL REFERENCES products(id),
    quantity    DECIMAL(10, 3) NOT NULL,
    unit_price  DECIMAL(10, 2) NOT NULL,
    notes       VARCHAR,
    created_at  TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
