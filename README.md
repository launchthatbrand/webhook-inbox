# Webhook Inbox

A webhook is stored once, and the outbound call is scheduled in the same database transaction. A repeat of the same source and `Idempotency-Key` returns the original event and does not add a second delivery.

The dispatcher locks one due outbox row with `FOR UPDATE SKIP LOCKED` and holds that lock while it calls the downstream URL. A second dispatcher cannot take the same row. A failed call increments the attempt count and sets a backoff. After `app.dispatch.max-attempts` the event is marked `DEAD`.

## Run

```bash
docker compose up -d
APP_OUTBOUND_URL=https://example.test/hooks mvn spring-boot:run
```

`POST /api/webhooks/{source}` requires the `Idempotency-Key` header and a JSON body. `GET /api/events/{id}` returns the status and attempt count. The app listens on port 8081. Postgres is published on host port 5433.
