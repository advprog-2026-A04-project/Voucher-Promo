package com.example.demo.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class DatabaseConnectivityTest extends MySqlTestcontainersBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void canConnectAndQueryVoucherTable() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vouchers", Integer.class);
        assertThat(count).isNotNull();
    }
}
