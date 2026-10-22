package com.desmondtatilian.webhookinbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(DispatchProperties.class)
public class WebhookInboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebhookInboxApplication.class, args);
    }
}
