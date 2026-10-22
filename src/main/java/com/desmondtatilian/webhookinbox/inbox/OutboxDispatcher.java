package com.desmondtatilian.webhookinbox.inbox;

import com.desmondtatilian.webhookinbox.DispatchProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxDispatcher {

    private final JdbcTemplate jdbc;
    private final OutboundClient outbound;
    private final DispatchProperties properties;

    public OutboxDispatcher(JdbcTemplate jdbc, OutboundClient outbound, DispatchProperties properties) {
        this.jdbc = jdbc;
        this.outbound = outbound;
        this.properties = properties;
    }

    @Transactional
    public boolean dispatchOnce() {
        Optional<LockedMessage> message = lockNext();
        if (message.isEmpty()) {
            return false;
        }
        LockedMessage locked = message.get();
        try {
            outbound.deliver(locked.source(), locked.payload());
            markDelivered(locked);
        } catch (RuntimeException exception) {
            markFailed(locked, exception);
        }
        return true;
    }

    private Optional<LockedMessage> lockNext() {
        return jdbc.query(
                """
                SELECT m.id AS outbox_id, e.id AS event_id, e.source, e.payload::text AS payload, e.attempt_count
                FROM outbox_messages m
                JOIN inbox_events e ON e.id = m.event_id
                WHERE m.status = 'PENDING' AND m.available_at <= now()
                ORDER BY m.available_at
                FOR UPDATE OF m SKIP LOCKED
                LIMIT 1
                """,
                (rs, row) -> map(rs))
                .stream()
                .findFirst();
    }

    private void markDelivered(LockedMessage message) {
        jdbc.update(
                "UPDATE inbox_events SET status = 'DELIVERED', updated_at = now() WHERE id = ?",
                message.eventId());
        jdbc.update(
                "UPDATE outbox_messages SET status = 'SENT' WHERE id = ?",
                message.outboxId());
    }

    private void markFailed(LockedMessage message, RuntimeException exception) {
        int attempts = message.attemptCount() + 1;
        String error = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        if (attempts >= properties.maxAttempts()) {
            jdbc.update(
                    """
                    UPDATE inbox_events
                    SET status = 'DEAD', attempt_count = ?, last_error = ?, updated_at = now()
                    WHERE id = ?
                    """,
                    attempts,
                    error,
                    message.eventId());
            jdbc.update("UPDATE outbox_messages SET status = 'DEAD' WHERE id = ?", message.outboxId());
            return;
        }
        Instant availableAt = Instant.now().plusMillis(properties.backoffMs() * attempts);
        jdbc.update(
                """
                UPDATE inbox_events
                SET attempt_count = ?, last_error = ?, updated_at = now()
                WHERE id = ?
                """,
                attempts,
                error,
                message.eventId());
        jdbc.update(
                "UPDATE outbox_messages SET available_at = ? WHERE id = ?",
                Timestamp.from(availableAt),
                message.outboxId());
    }

    private static LockedMessage map(ResultSet rs) throws SQLException {
        return new LockedMessage(
                rs.getObject("outbox_id", UUID.class),
                rs.getObject("event_id", UUID.class),
                rs.getString("source"),
                rs.getString("payload"),
                rs.getInt("attempt_count"));
    }

    private record LockedMessage(UUID outboxId, UUID eventId, String source, String payload, int attemptCount) {
    }
}
