package com.example.demo.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {

    @Test
    void health_whenDbConnectionValid_returnsUp() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        HealthController controller = new HealthController(dataSource);

        ResponseEntity<Map<String, Object>> resp = controller.health();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).containsEntry("status", "UP").containsEntry("db", "UP");
    }

    @Test
    void health_whenDbConnectionInvalid_returnsDown() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(false);

        HealthController controller = new HealthController(dataSource);

        ResponseEntity<Map<String, Object>> resp = controller.health();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody()).containsEntry("status", "DOWN").containsEntry("db", "DOWN");
    }

    @Test
    void health_whenSQLException_returnsDown() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("db down"));

        HealthController controller = new HealthController(dataSource);

        ResponseEntity<Map<String, Object>> resp = controller.health();
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(resp.getBody()).containsEntry("status", "DOWN").containsEntry("db", "DOWN");
    }
}

