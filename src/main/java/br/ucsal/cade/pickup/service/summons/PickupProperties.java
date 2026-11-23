package br.ucsal.cade.pickup.service.summons;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Endereço e horário da retirada, de {@code cade.pickup.*}. Conferidos na subida: um valor ausente impede a aplicação de
 * subir.
 *
 * @param place    onde fica a sala de achados e perdidos
 * @param hours    horário de atendimento da sala (RF26)
 */
@Validated
@ConfigurationProperties("cade.pickup")
public record PickupProperties(@NotBlank String place, @NotBlank String hours) {
}
