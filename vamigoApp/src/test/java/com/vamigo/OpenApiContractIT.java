package com.vamigo;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract smoke-test for the generated OpenAPI document.
 *
 * <p>Guarded with {@link Assumptions#assumeTrue(boolean)} so the test skips cleanly when
 * springdoc-openapi is absent from the classpath (rather than silently passing or failing
 * for the wrong reason). The project currently does not ship springdoc; this test becomes
 * live the moment the dependency is added.
 */
class OpenApiContractIT extends AbstractFullStackTest {

    private static final String SPRINGDOC_WEBMVC_CLASS =
            "org.springdoc.webmvc.api.OpenApiWebMvcResource";

    @BeforeAll
    static void requireSpringdoc() {
        Assumptions.assumeTrue(
                ClassUtils.isPresent(SPRINGDOC_WEBMVC_CLASS, OpenApiContractIT.class.getClassLoader()),
                "springdoc-openapi not on classpath — skipping OpenAPI contract check");
    }

    @Test
    void apiDocsEndpoint_returnsWellFormedOpenApi() {
        var result = mvc.get().uri("/v3/api-docs").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.openapi").asString().startsWith("3.");
        assertThat(result).bodyJson().extractingPath("$.info.title").asString().isNotBlank();
        assertThat(result).bodyJson().extractingPath("$.paths").asMap().isNotEmpty();
    }
}
