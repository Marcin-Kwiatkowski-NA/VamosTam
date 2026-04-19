package com.vamigo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    @Primary
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean("localBusinessClock")
    public Clock localBusinessClock(@Value("${search-alert.expiry-zone:Europe/Warsaw}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
