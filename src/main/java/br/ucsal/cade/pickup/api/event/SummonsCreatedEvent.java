package br.ucsal.cade.pickup.api.event;

import br.ucsal.cade.pickup.api.PickupInstructions;

import java.util.Objects;
import java.util.UUID;

/**
 * Um estudante foi convocado (o {@code convocacao.criada} da seção 7.2 dos requisitos). Publicado na transação da
 * convocação; o e-mail com local, horário, prazo e o que levar (AVI) ouve depois do commit, e uma falha nele não
 * desfaz a convocação (RNF08).
 *
 * @param studentId    id do estudante convocado, dono do registro de perda
 * @param instructions o que vai no e-mail; nada reservado do objeto (RN03)
 */
public record SummonsCreatedEvent(UUID summonsId, UUID studentId, UUID foundItemId, UUID lossReportId,
                                  PickupInstructions instructions) {

    public SummonsCreatedEvent {
        Objects.requireNonNull(summonsId);
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(foundItemId);
        Objects.requireNonNull(lossReportId);
        Objects.requireNonNull(instructions);
    }
}
