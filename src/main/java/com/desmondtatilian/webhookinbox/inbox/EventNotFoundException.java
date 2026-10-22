package com.desmondtatilian.webhookinbox.inbox;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EventNotFoundException extends ResponseStatusException {

    public EventNotFoundException(UUID id) {
        super(HttpStatus.NOT_FOUND, "Event not found");
    }
}
