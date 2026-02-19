package com.blablatwo;

import com.blablatwo.config.JpaConfig;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer postgis = new PostgreSQLContainer(
            DockerImageName.parse("imresamu/postgis:18-3.6")
                    .asCompatibleSubstituteFor("postgres")
    );

    static {
        postgis.start();
    }
}
