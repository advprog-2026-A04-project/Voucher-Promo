package com.example.demo.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock(@Value("${app.time-zone:}") String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return Clock.systemDefaultZone();
        }
        return Clock.system(ZoneId.of(timeZone.trim()));
    }
}

