package br.ucsal.cade.shared.time;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/**
 * Troca o relógio da aplicação por um {@link MutableClock} que os testes podem adiantar. O bean é compartilhado por
 * toda classe de teste que reaproveita o mesmo contexto, então os testes calculam as horas a partir de
 * {@code clock.instant()}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClockConfiguration {

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-11-14T12:00:00Z"));
    }
}
