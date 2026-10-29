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
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);