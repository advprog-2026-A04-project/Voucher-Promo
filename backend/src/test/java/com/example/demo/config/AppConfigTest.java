package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AppConfigTest {

    @Test
    void clock_whenPropertyBlank_usesSystemDefaultZone() {
        AppConfig config = new AppConfig();
        Clock clock = config.clock("   ");

        assertThat(clock.getZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void clock_whenPropertyProvided_usesConfiguredZone() {
        AppConfig config = new AppConfig();
        Clock clock = config.clock("Asia/Jakarta");

        assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Jakarta"));
    }
}

