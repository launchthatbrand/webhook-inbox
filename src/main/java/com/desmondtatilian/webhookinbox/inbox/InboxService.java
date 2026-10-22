package com.desmondtatilian.webhookinbox.inbox;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class InboxService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public InboxService(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(transactions);
    }

    public AcceptedEvent accept(String source, String idempotencyKey, String payload) {
        try {
            return transactions.execute(status -> insert(source, idempotencyKey, payload));
        } catch (DataIntegrityViolationException exception) {
            return find(source, idempotencyKey);
        }
    }

    public AcceptedEvent find(String source, String idempotencyKey) {
        return jdbc.queryForObject(
                """
                SELECT id, source, status, attempt_count
                FROM inbox_events
                WHERE source = ? AND idempotency_key = ?
                """,
                (rs, row) -> new AcceptedEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("source"),
                        rs.getString("status"),
                        rs.getInt("attempt_count"),
                        true),
                source,
                idempotencyKey);
    }

    public AcceptedEvent get(UUID id) {
        return jdbc.query(
                """
                SELECT id, source, status, attempt_count
                FROM inbox_events
                WHERE id = ?
                """,
                (rs, row) -> new AcceptedEvent(
                        rs.getObject("id", UUID.class),
                        rs.getString("source"),
                        rs.getString("status"),
                        rs.getInt("attempt_count"),
                        false),
                id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    public int outboxCount(UUID eventId) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM outbox_messages WHERE event_id = ?",
                Integer.class,
                eventId);
        return count == null ? 0 : count;
    }

    private AcceptedEvent insert(String source, String idempotencyKey, String payload) {
        UUID eventId = UUID.randomUUID();
        jdbc.update(
                """
                INSERT INTO inbox_events
                    (id, source, idempotency_key, payload, status, attempt_count, created_at, updated_at)
                VALUES (?, ?, ?, ?::jsonb, 'ACCEPTED', 0, now(), now())
                """,
                eventId,
                source,
                idempotencyKey,
                payload);
        jdbc.update(
                """
                INSERT INTO outbox_messages (id, event_id, status, available_at, created_at)
                VALUES (?, ?, 'PENDING', now(), now())
                """,
                UUID.randomUUID(),
                eventId);
        return new AcceptedEvent(eventId, source, "ACCEPTED", 0, false);
    }
}
