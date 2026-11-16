package br.ucsal.cade.shared.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Relógio da aplicação no fuso de Salvador. Tudo que precisa da hora atual recebe o {@link Clock} por injeção, para os
 * testes de prazo poderem usar um relógio controlado.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    public static final ZoneId ZONE = ZoneId.of("America/Bahia");

    @Bean
    Clock clock() {
        return Clock.system(ZONE);
    }
}
