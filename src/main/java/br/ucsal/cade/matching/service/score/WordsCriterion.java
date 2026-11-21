package br.ucsal.cade.matching.service.score;

import java.util.Set;

/**
 * Palavras em comum entre as descrições, até um limite (RN07). Não contam palavras curtas, palavras vazias ("de",
 * "com") nem as do nome da categoria, que os dois lados já têm em comum (D22).
 */
public record WordsCriterion(MatchingProperties.Words weights) implements CompatibilityCriterion {

    @Override
    public int score(MatchCandidate candidate) {
        Set<String> ignored = Text.words(candidate.foundItem().categoryName(), 1, Set.of());
        Set<String> found = Text.words(candidate.foundItem().description(), weights.minLength(), ignored);
        Set<String> lost = Text.words(candidate.lossReport().description(), weights.minLength(), ignored);
        long common = found.stream().filter(lost::contains).count();
        return (int) Math.min(common, weights.maxCounted()) * weights.pointsEach();
    }
}
