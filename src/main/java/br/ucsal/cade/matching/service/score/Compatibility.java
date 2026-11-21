package br.ucsal.cade.matching.service.score;

import java.util.Objects;

/** A nota de 0 a 100 de um par objeto × perda e a faixa dela (RN07). */
public record Compatibility(int score, CompatibilityBand band) {

    public Compatibility {
        if (score < 0 || score > SumOfCriteria.MAX_SCORE) {
            throw new IllegalArgumentException("Nota fora de 0 a 100: " + score);
        }
        Objects.requireNonNull(band);
    }
}
