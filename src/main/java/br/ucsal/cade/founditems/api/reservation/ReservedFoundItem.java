package br.ucsal.cade.founditems.api.reservation;

import br.ucsal.cade.shared.category.RiskLevel;

import java.util.Objects;
import java.util.UUID;

/**
 * O objeto recém-reservado, com o que a convocação precisa: o risco define o que o estudante leva ao balcão (RN09).
 * Não leva foto, descrição interna nem detalhes reservados (RN03).
 */
public record ReservedFoundItem(UUID id, RiskLevel risk) {

    public ReservedFoundItem {
        Objects.requireNonNull(id);
        Objects.requireNonNull(risk);
    }
}
