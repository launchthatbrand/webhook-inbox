package com.desmondtatilian.webhookinbox.inbox;

import com.desmondtatilian.webhookinbox.DispatchProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestOutboundClient implements OutboundClient {

    private final RestClient client;
    private final String url;

    public RestOutboundClient(RestClient.Builder builder, DispatchProperties properties) {
        this.client = builder.build();
        this.url = properties.outboundUrl();
    }

    @Override
    public void deliver(String source, String payload) {
        client.post()
                .uri(url)
                .header("X-Webhook-Source", source)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
    }
}
