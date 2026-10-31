CREATE SCHEMA IF NOT EXISTS accounts;

CREATE TABLE IF NOT EXISTS accounts.accounts (
    id           BIGSERIAL   PRIMARY KEY,
    login        VARCHAR(64) NOT NULL UNIQUE,
    first_name   VARCHAR(64) NOT NULL,
    last_name    VARCHAR(64) NOT NULL,
    birth_date   DATE        NOT NULL,
    balance      NUMERIC(19, 2) NOT NULL DEFAULT 0 CHECK (balance >= 0)
);

CREATE INDEX IF NOT EXISTS idx_accounts_login ON accounts(login);

CREATE TABLE IF NOT EXISTS transactions (
    id VARCHAR(128) PRIMARY KEY,
    created_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS outbox (
    id            BIGSERIAL    PRIMARY KEY,
    login         VARCHAR(64)  NOT NULL,
    payload       JSONB        NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count   INT          NOT NULL DEFAULT 0,
    update_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox (id) WHERE status = 'PENDING';