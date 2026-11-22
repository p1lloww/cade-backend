package br.ucsal.cade.lossreports.service.report;

import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.period.Shift;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Pedido de correção de um registro de perda, como o estudante enviou (PER-07). Leva os mesmos campos do registro,
 * menos a foto, e a versão que ele viu: se o registro mudou desde então (por exemplo, foi convocado), a correção é
 * recusada com 409.
 *
 * @param version a {@code version} do registro na última vez que o estudante o recebeu
 */
public record UpdateLossReport(UUID categoryId, String description, String secretDetail, List<LossLocation> locations,
                               LocalDate lostFrom, LocalDate lostUntil, Shift shift, long version) {

    public UpdateLossReport {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(lostFrom);
        locations = locations == null ? List.of() : List.copyOf(locations);
    }
}
