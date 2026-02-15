package com.blablatwo.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

@Component
public class BrevoClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrevoClient.class);

    private final RestClient restClient;

    public BrevoClient(@Value("${brevo.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
    }

    public void sendTemplateEmail(String toEmail, String toName,
                                  long templateId, Map<String, String> params) {
        var body = Map.of(
                "to", List.of(Map.of("email", toEmail, "name", toName)),
                "templateId", templateId,
                "params", params
        );

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            LOGGER.info("Template email {} sent to {}", templateId, toEmail);
        } catch (RestClientException e) {
            LOGGER.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new EmailSendException("Failed to send verification email", e);
        }
    }
}
