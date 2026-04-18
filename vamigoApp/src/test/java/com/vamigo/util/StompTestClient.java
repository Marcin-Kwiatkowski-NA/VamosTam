package com.vamigo.util;

import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Blocking STOMP subscription wrapper — collects frames into a queue so tests can await them via
 * Awaitility. Type is captured via a concrete Class token (no reflective generics).
 */
public final class StompTestClient {

    private StompTestClient() {}

    public static <T> BlockingQueue<T> subscribe(StompSession session, String destination, Class<T> payloadType) {
        BlockingQueue<T> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((T) payload);
            }
        });
        return queue;
    }
}
