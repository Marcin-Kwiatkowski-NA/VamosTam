package com.blablatwo;

import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
