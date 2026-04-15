CREATE TABLE daily_prices (
    id            UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id     UUID           NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    product_id    UUID           NOT NULL REFERENCES products(id),
    date          DATE           NOT NULL,
    unit_cost     DECIMAL(10, 2) NOT NULL,
    selling_price DECIMAL(10, 2) NOT NULL,
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_daily_price UNIQUE (tenant_id, product_id, date)
);

CREATE INDEX idx_daily_prices_tenant_date ON daily_prices(tenant_id, date);
