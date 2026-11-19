package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.Shift;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pedido de registro de uma perda, como o estudante enviou.
 *
 * @param secretDetail detalhe que só o dono conhece; nulo ou vazio se ele não informar (RF14)
 * @param locations    locais onde acha que perdeu; vazia quando não sabe
 * @param lostUntil    fim do período; nulo quando ele informa uma data só
 * @param shift        turno; nulo quando ele não lembra
 */
public record RegisterLossReport(UUID categoryId, String description, String secretDetail,
                                 List<LossLocation> locations, LocalDate lostFrom, LocalDate lostUntil, Shift shift) {

    public RegisterLossReport {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(lostFrom);
        locations = locations == null ? List.of() : List.copyOf(locations);
    }
}
