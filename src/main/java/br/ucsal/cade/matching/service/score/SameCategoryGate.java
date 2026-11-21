package br.ucsal.cade.matching.service.score;

import java.util.Objects;

/** Categoria diferente zera a nota; categoria igual não soma pontos, só libera os demais critérios (RN07). */
public record SameCategoryGate(CompatibilityCriterion criteria) implements CompatibilityCriterion {

    public SameCategoryGate {
        Objects.requireNonNull(criteria);
    }

    @Override
    public int score(MatchCandidate candidate) {
        boolean sameCategory = candidate.foundItem().categoryId().equals(candidate.lossReport().categoryId());
        return sameCategory ? criteria.score(candidate) : 0;
    }
}
