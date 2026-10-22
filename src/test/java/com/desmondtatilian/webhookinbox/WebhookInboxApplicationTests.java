package com.desmondtatilian.webhookinbox;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.desmondtatilian.webhookinbox.inbox.InboxService;
import com.desmondtatilian.webhookinbox.inbox.OutboundClient;
import com.desmondtatilian.webhookinbox.inbox.OutboxDispatcher;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "app.dispatch.enabled=false",
        "app.dispatch.max-attempts=3",
        "app.dispatch.backoff-ms=0"
})
@AutoConfigureMockMvc
@Testcontainers
class WebhookInboxApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    MockMvc mvc;

    @Autowired
    OutboxDispatcher dispatcher;

    @Autowired
    InboxService inbox;

    @MockitoBean
    OutboundClient outbound;

    @Test
    void duplicateKeyStoresOneOutboxRow() throws Exception {
        String first = postEvent("payments", "key-1");
        String second = postEvent("payments", "key-1");

        org.junit.jupiter.api.Assertions.assertEquals(first, second);
        org.junit.jupiter.api.Assertions.assertEquals(1, inbox.outboxCount(UUID.fromString(first)));
    }

    @Test
    void failedDeliveryIsRetried() throws Exception {
        doThrow(new IllegalStateException("downstream down"))
                .doNothing()
                .when(outbound).deliver(anyString(), anyString());

        String id = postEvent("payments", "key-2");

        org.junit.jupiter.api.Assertions.assertTrue(dispatcher.dispatchOnce());
        mvc.perform(get("/api/events/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.attemptCount").value(1));

        org.junit.jupiter.api.Assertions.assertTrue(dispatcher.dispatchOnce());
        mvc.perform(get("/api/events/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    private String postEvent(String source, String key) throws Exception {
        MvcResult result = mvc.perform(post("/api/webhooks/" + source)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invoice\":42}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
