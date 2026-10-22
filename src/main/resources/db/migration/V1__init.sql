CREATE TABLE inbox_events (
    id uuid PRIMARY KEY,
    source text NOT NULL,
    idempotency_key text NOT NULL,
    payload jsonb NOT NULL,
    status text NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0,
    last_error text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (source, idempotency_key)
);

CREATE TABLE outbox_messages (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES inbox_events (id),
    status text NOT NULL,
    available_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL
);

CREATE INDEX outbox_pending_idx ON outbox_messages (available_at) WHERE status = 'PENDING';
