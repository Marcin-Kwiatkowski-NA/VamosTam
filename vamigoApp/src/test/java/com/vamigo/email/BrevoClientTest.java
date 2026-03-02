package com.vamigo.email;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BrevoClientTest {

    @Test
    void constructor_createsClientWithApiKey() {
        // Verify client can be instantiated without throwing
        BrevoClient client = new BrevoClient("test-api-key");
        assertThat(client).isNotNull();
    }

    @Test
    void sendTemplateEmail_throwsEmailSendExceptionOnFailure() {
        // Use an invalid base URL to trigger RestClientException
        BrevoClient client = new BrevoClient("invalid-key");

        assertThatThrownBy(() -> client.sendTemplateEmail(
                "test@example.com",
                "Test User",
                1L,
                Map.of("KEY", "VALUE")
        )).isInstanceOf(EmailSendException.class);
    }
}
