package br.ucsal.cade.matching.service.score;

import java.util.List;

/** Soma os critérios como se fossem um só, sem passar de 100 (RN07). */
public record SumOfCriteria(List<CompatibilityCriterion> criteria) implements CompatibilityCriterion {

    public static final int MAX_SCORE = 100;

    public SumOfCriteria {
        criteria = List.copyOf(criteria);
    }

    @Override
    public int score(MatchCandidate candidate) {
        int sum = criteria.stream().mapToInt(criterion -> criterion.score(candidate)).sum();
        return Math.min(sum, MAX_SCORE);
    }
}
