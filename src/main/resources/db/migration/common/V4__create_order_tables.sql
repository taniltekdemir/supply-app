CREATE TABLE orders (
    id          UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    customer_id UUID        NOT NULL REFERENCES customers(id),
    order_date  DATE        NOT NULL,
    status      VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX idx_orders_tenant_date ON orders(tenant_id, order_date);

CREATE TABLE order_items (
    id         UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id  UUID           NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    order_id   UUID           NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID           NOT NULL REFERENCES products(id),
    quantity   DECIMAL(10, 3) NOT NULL,
    notes      VARCHAR,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
