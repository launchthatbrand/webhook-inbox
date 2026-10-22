package com.desmondtatilian.webhookinbox.inbox;

import java.util.UUID;

public record AcceptedEvent(
        UUID id,
        String source,
        String status,
        int attemptCount,
        boolean duplicate) {
}
