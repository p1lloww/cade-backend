package br.ucsal.cade.retention.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Prazo de guarda e de quanto em quanto tempo a tarefa procura os objetos vencidos. O prazo é de 90 dias desde a
 * entrada na sala (RN16, validado com o administrador).
 */
@Validated
@ConfigurationProperties("cade.retention")
public record RetentionProperties(@NotNull Duration storagePeriod, @NotNull Duration interval) {
}
