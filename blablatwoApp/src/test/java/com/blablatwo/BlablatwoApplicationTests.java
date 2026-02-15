package com.blablatwo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BlablatwoApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // checks context loading
    }
}
