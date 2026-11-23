package br.ucsal.cade.retention.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * De quanto em quanto tempo a tarefa procura os objetos vencidos. O prazo de guarda não fica aqui: é o que o
 * administrador configurou (RF45, D39), lido em {@code Deadlines} a cada rodada.
 */
@Validated
@ConfigurationProperties("cade.retention")
public record RetentionProperties(@NotNull Duration interval) {
}
