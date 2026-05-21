package com.example.demo.it;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;

public abstract class MySqlTestcontainersBase {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("voucherpromo_test")
            .withUsername("test")
            .withPassword("test");
    private static final boolean MYSQL_AVAILABLE = startMysqlIfDockerIsAvailable();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        if (MYSQL_AVAILABLE) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        }
        registry.add("app.admin-token", () -> "dev-admin-token");
    }

    private static boolean startMysqlIfDockerIsAvailable() {
        try {
            if (!DockerClientFactory.instance().isDockerAvailable()) {
                return false;
            }
            MYSQL.start();
            return true;
        } catch (IllegalStateException | ContainerLaunchException exception) {
            return false;
        }
    }
}
