package com.expensetracker.integration;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base for integration tests: a real MySQL 8.4 in Docker, schema created by the Flyway migrations and checked by
 * Hibernate (ddl-auto=validate). Skipped automatically when Docker is not available.
 *
 * <p>The Testcontainers extension stops the container after each test class, which would leave a cached Spring
 * context pointing at a dead port. {@code @DirtiesContext} drops the context with it, so each class gets a fresh pair.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {"app.seed-demo=false", "app.mail.enabled=false"})
abstract class AbstractMySqlIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");
}
