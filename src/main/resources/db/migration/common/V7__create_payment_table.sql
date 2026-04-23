CREATE TABLE payments (
    id             UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id      UUID           NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    invoice_id     UUID           NOT NULL REFERENCES invoices(id),
    payment_method VARCHAR(10)    NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    is_paid        BOOLEAN        NOT NULL DEFAULT false,
    payment_date   DATE           NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_tenant_id   ON payments(tenant_id);
CREATE INDEX idx_payments_tenant_date ON payments(tenant_id, payment_date);
CREATE INDEX idx_payments_invoice_id  ON payments(invoice_id);
