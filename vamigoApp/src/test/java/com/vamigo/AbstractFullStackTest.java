package com.vamigo;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

/**
 * Full-stack integration test base: {@code @SpringBootTest} (MOCK env) + {@link MockMvcTester}
 * + singleton PostGIS + declaratively-configured WireMock.
 *
 * <p>WireMock is wired via {@code wiremock-spring-boot}'s {@link EnableWireMock} — the framework
 * starts the server on port 19089 (matches {@code photon.url} in {@code application-test.properties}),
 * resets stubs and the request journal between tests, and exposes the instance for injection via
 * {@code @InjectWireMock("photon-mock")} in subclasses that need to stub or verify.
 *
 * <p>Does <b>not</b> carry {@code @RecordApplicationEvents} — subclasses that need primary-event
 * capture must annotate themselves to avoid forcing the overhead on every full-stack IT.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnableWireMock({@ConfigureWireMock(port = 19089, name = "photon-mock")})
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractFullStackTest extends AbstractIntegrationTest {

    @Autowired
    protected MockMvcTester mvc;
}
