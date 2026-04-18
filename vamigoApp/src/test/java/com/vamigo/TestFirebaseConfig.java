package com.vamigo;

import com.google.firebase.auth.FirebaseAuth;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Supplies a mock {@link FirebaseAuth} bean for integration tests where {@code firebase.enabled=false}
 * keeps the real {@code FirebaseConfig} off. {@link com.vamigo.auth.service.FirebaseTokenVerifier}
 * requires a {@code FirebaseAuth} dependency; tests that exercise Facebook auth override this
 * mock via {@code @MockitoBean FirebaseTokenVerifier} directly.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestFirebaseConfig {

    @Bean
    @ConditionalOnMissingBean
    public FirebaseAuth firebaseAuth() {
        return Mockito.mock(FirebaseAuth.class);
    }
}
