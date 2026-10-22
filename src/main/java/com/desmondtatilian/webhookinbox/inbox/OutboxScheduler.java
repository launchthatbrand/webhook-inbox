package com.desmondtatilian.webhookinbox.inbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.dispatch.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxDispatcher dispatcher;

    public OutboxScheduler(OutboxDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Scheduled(fixedDelayString = "${app.dispatch.delay-ms:1000}")
    public void tick() {
        while (dispatcher.dispatchOnce()) {
            // Drain the due messages. The row lock keeps a second tick from taking the same one.
        }
    }
}
