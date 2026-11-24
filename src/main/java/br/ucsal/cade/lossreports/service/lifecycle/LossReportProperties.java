package br.ucsal.cade.lossreports.service.lifecycle;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Prazos do registro de perda, de {@code cade.loss-reports.*}.
 *
 * @param infoResponseDeadline quanto tempo o estudante tem para responder a um pedido de informações (RN13)
 */
@Validated
@ConfigurationProperties("cade.loss-reports")
public record LossReportProperties(@NotNull Duration infoResponseDeadline) {
}
