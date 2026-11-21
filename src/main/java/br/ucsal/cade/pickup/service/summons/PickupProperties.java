package br.ucsal.cade.pickup.service.summons;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Prazo e endereço da retirada, de {@code cade.pickup.*}. Conferidos na subida: um valor ausente impede a aplicação de
 * subir.
 *
 * @param deadline quanto tempo o objeto fica reservado depois da convocação (RN15)
 * @param place    onde fica a sala de achados e perdidos
 * @param hours    horário de atendimento da sala (RF26)
 */
@Validated
@ConfigurationProperties("cade.pickup")
public record PickupProperties(@NotNull Duration deadline, @NotBlank String place, @NotBlank String hours) {
}
