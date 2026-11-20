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
 * @param locations vazia quando o estudante não sabe onde perdeu
 * @param lostUntil nula quando é uma data só
 * @param photo     nula quando não há foto
 */
public record RegisterLossReport(UUID categoryId, String description, String secretDetail,
                                 List<LossLocation> locations, LocalDate lostFrom, LocalDate lostUntil, Shift shift,
                                 byte[] photo) {

    public RegisterLossReport {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(lostFrom);
        locations = locations == null ? List.of() : List.copyOf(locations);
    }

    public boolean hasPhoto() {
        return photo != null && photo.length > 0;
    }
}
