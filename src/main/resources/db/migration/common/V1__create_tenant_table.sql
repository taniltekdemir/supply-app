CREATE TABLE tenants (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name          VARCHAR      NOT NULL,
    email         VARCHAR      NOT NULL,
    password_hash VARCHAR      NOT NULL,
    plan          VARCHAR(10)  NOT NULL DEFAULT 'FREE',
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenants_email UNIQUE (email)
);
