package com.desmondtatilian.webhookinbox.inbox;

public interface OutboundClient {

    void deliver(String source, String payload);
}
