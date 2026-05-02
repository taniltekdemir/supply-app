ALTER TABLE invoices
    ADD COLUMN order_id UUID,
    ADD CONSTRAINT fk_invoices_order FOREIGN KEY (order_id) REFERENCES orders (id);
