package com.desmondtatilian.webhookinbox.api;

import com.desmondtatilian.webhookinbox.inbox.AcceptedEvent;
import com.desmondtatilian.webhookinbox.inbox.InboxService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class WebhookController {

    private final InboxService inbox;

    public WebhookController(InboxService inbox) {
        this.inbox = inbox;
    }

    @PostMapping("/webhooks/{source}")
    public ResponseEntity<AcceptedEvent> accept(
            @PathVariable String source,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody String payload) {
        AcceptedEvent event = inbox.accept(source, idempotencyKey, payload);
        HttpStatus status = event.duplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(event);
    }

    @GetMapping("/events/{id}")
    public AcceptedEvent get(@PathVariable UUID id) {
        return inbox.get(id);
    }
}
