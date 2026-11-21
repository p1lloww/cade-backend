package br.ucsal.cade.pickup.api;

import java.time.Instant;
import java.util.Objects;

/**
 * O que o estudante convocado precisa saber para retirar o objeto (RF26): onde, em que horário, até quando e o que
 * levar. Não revela foto, descrição interna nem detalhes reservados do objeto (RN03).
 *
 * @param place       onde fica a sala de achados e perdidos
 * @param hours       horário de atendimento da sala, como texto
 * @param deadline    até quando o objeto fica reservado (RN15)
 * @param whatToBring o que levar ao balcão, conforme o risco do objeto (RN09, RN10)
 */
public record PickupInstructions(String place, String hours, Instant deadline, String whatToBring) {

    public PickupInstructions {
        Objects.requireNonNull(place);
        Objects.requireNonNull(hours);
        Objects.requireNonNull(deadline);
        Objects.requireNonNull(whatToBring);
    }
}
