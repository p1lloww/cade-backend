package br.ucsal.cade.shared.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Application clock in the Salvador time zone. Anything that needs the current time gets the {@link Clock} injected,
 * so deadline tests can use a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    public static final ZoneId ZONE = ZoneId.of("America/Bahia");

    @Bean
    Clock clock() {
        return Clock.system(ZONE);
    }
}
