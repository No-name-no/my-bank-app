CREATE SCHEMA IF NOT EXISTS cash;

CREATE TABLE IF NOT EXISTS outbox (
    id            BIGSERIAL    PRIMARY KEY,
    login         VARCHAR(64)  NOT NULL,
    payload       JSONB        NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count   INT          NOT NULL DEFAULT 0,
    update_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox (id) WHERE status = 'PENDING';