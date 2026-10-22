package com.desmondtatilian.webhookinbox;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record DispatchProperties(Outbound outbound, Dispatch dispatch) {

    public String outboundUrl() {
        return outbound.url();
    }

    public boolean dispatchEnabled() {
        return dispatch.enabled();
    }

    public int maxAttempts() {
        return dispatch.maxAttempts();
    }

    public long backoffMs() {
        return dispatch.backoffMs();
    }

    public record Outbound(String url) {
    }

    public record Dispatch(boolean enabled, long delayMs, int maxAttempts, long backoffMs) {
    }
}
