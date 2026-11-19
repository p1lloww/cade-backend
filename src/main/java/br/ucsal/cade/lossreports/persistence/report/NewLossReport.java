package br.ucsal.cade.lossreports.persistence.report;

import br.ucsal.cade.shared.category.Category;

import java.util.List;
import java.util.Objects;

/**
 * Dados de uma perda a registrar, já com a categoria encontrada e o período validado.
 *
 * @param secretDetail detalhe que só o dono conhece; nulo se o estudante não informar
 * @param locations    locais onde acha que perdeu; vazia quando não sabe
 */
public record NewLossReport(Category category, String description, String secretDetail,
                            List<LossLocation> locations, LostPeriod period) {

    public NewLossReport {
        Objects.requireNonNull(category);
        Objects.requireNonNull(description);
        Objects.requireNonNull(period);
        locations = locations == null ? List.of() : List.copyOf(locations);
    }
}
