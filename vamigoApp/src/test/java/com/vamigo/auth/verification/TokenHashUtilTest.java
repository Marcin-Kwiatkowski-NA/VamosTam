package com.vamigo.auth.verification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHashUtilTest {

    @Test
    void generateToken_returnsUrlSafeBase64() {
        String token = TokenHashUtil.generateToken();

        assertThat(token).isNotBlank();
        assertThat(token).hasSize(43); // 32 bytes → 43 chars URL-safe Base64 without padding
        assertThat(token).matches("[A-Za-z0-9_-]+");
    }

    @Test
    void generateToken_producesUniqueTokens() {
        String token1 = TokenHashUtil.generateToken();
        String token2 = TokenHashUtil.generateToken();

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void hashToken_returnsSha256HexDigest() {
        String hash = TokenHashUtil.hashToken("test-token");

        assertThat(hash).hasSize(64); // SHA-256 = 32 bytes = 64 hex chars
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    void hashToken_isDeterministic() {
        String hash1 = TokenHashUtil.hashToken("same-input");
        String hash2 = TokenHashUtil.hashToken("same-input");

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hashToken_differentInputsProduceDifferentHashes() {
        String hash1 = TokenHashUtil.hashToken("input-a");
        String hash2 = TokenHashUtil.hashToken("input-b");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
