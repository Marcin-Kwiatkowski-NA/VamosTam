package com.vamigo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Base class for STOMP tests that need a real HTTP port.
 *
 * <p><b>Filter-bypass trap:</b> do <i>not</i> reintroduce {@code @AutoConfigureRestTestClient}. That
 * annotation binds the test client to the {@code WebApplicationContext} and short-circuits the servlet
 * filter chain — {@code JwtAuthenticationFilter} never runs, so auth-related assertions silently pass.
 * Tests that need HTTP should build a {@code RestTestClient} via
 * {@code RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build()} so traffic goes
 * through the real servlet stack.
 *
 * <p>Do <b>not</b> mix {@code MockMvcTester} with the real port — that'd give you two different servlet
 * paths in one test class.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractStompTest extends AbstractIntegrationTest {

    @LocalServerPort
    protected int port;

    /** Connect a STOMP session with the given JWT in the CONNECT frame Authorization header. */
    protected StompSession connectStomp(String jwt)
            throws ExecutionException, InterruptedException, TimeoutException {
        return connectStomp(jwt, new StompSessionHandlerAdapter() {});
    }

    protected StompSession connectStomp(String jwt, StompSessionHandlerAdapter handler)
            throws ExecutionException, InterruptedException, TimeoutException {
        WebSocketClient webSocketClient = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(webSocketClient);
        stompClient.setMessageConverter(new JacksonJsonMessageConverter());

        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("stomp-heartbeat-test-");
        taskScheduler.initialize();
        stompClient.setTaskScheduler(taskScheduler);
        stompClient.setDefaultHeartbeat(new long[]{0, 0});

        String url = "ws://localhost:" + port + "/ws";

        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();
        StompHeaders stompHeaders = new StompHeaders();
        if (jwt != null) {
            stompHeaders.add("Authorization", "Bearer " + jwt);
        }

        return stompClient.connectAsync(url, wsHeaders, stompHeaders, handler)
                .get(5, TimeUnit.SECONDS);
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}
