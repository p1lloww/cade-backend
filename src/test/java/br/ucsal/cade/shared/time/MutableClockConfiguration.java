package br.ucsal.cade.shared.time;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/**
 * Replaces the application clock with a {@link MutableClock} that tests can move forward. The bean is shared by
 * every test class that reuses the same context, so tests should compute times from {@code clock.instant()}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClockConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-11-14T12:00:00Z"));
    }
}
