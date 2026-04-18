package com.vamigo.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.vamigo.auth.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FirebaseTokenVerifierTest {

    @Test
    void verify_delegatesToFirebaseAuth() throws FirebaseAuthException {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        FirebaseToken expected = mock(FirebaseToken.class);
        when(firebaseAuth.verifyIdToken(eq("good-token"))).thenReturn(expected);

        FirebaseTokenVerifier verifier = new FirebaseTokenVerifier(firebaseAuth);

        assertThat(verifier.verify("good-token")).isSameAs(expected);
    }

    @Test
    void verify_throwsInvalidTokenException_whenFirebaseAuthFails() throws FirebaseAuthException {
        FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
        when(firebaseAuth.verifyIdToken(eq("bad-token")))
                .thenThrow(mock(FirebaseAuthException.class));

        FirebaseTokenVerifier verifier = new FirebaseTokenVerifier(firebaseAuth);

        assertThatThrownBy(() -> verifier.verify("bad-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid Firebase ID token");
    }
}
