package br.ucsal.cade.lossreports.api.registration;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * O que um registro de perda precisa, para quem registra por código (o modo demonstração, RF47), sem passar pela rota
 * do estudante.
 *
 * @param secretDetail o detalhe que só o dono conhece (RF14); pode ser nulo
 * @param block        onde o estudante acha que perdeu; nulo (com a sala também nula) quando ele não sabe
 * @param room         sala do bloco; pode ser nula
 */
public record LossReportData(UUID categoryId, String description, String secretDetail, String block, String room,
                             LocalDate lostOn) {

    public LossReportData {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(lostOn);
    }
}
