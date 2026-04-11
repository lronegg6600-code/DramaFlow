CREATE TABLE IF NOT EXISTS operation_idempotency_keys (
    operation_scope TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    status TEXT NOT NULL,
    response_payload JSONB,
    error_payload JSONB,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (operation_scope, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_operation_idempotency_expires_at
ON operation_idempotency_keys (expires_at);

CREATE INDEX IF NOT EXISTS idx_operation_idempotency_status
ON operation_idempotency_keys (status);
