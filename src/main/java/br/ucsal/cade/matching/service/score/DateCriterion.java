package br.ucsal.cade.matching.service.score;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Quanto mais perto da perda o objeto foi achado, mais pontos (RN07). Num período, a duração conta como incerteza:
 * os dias são o maior entre a duração e os dias depois do fim (D22). Uma data só tem duração zero.
 */
public record DateCriterion(MatchingProperties.Date weights) implements CompatibilityCriterion {

    @Override
    public int score(MatchCandidate candidate) {
        LocalDate foundOn = candidate.foundItem().foundOn();
        LocalDate lostFrom = candidate.lossReport().lostFrom();
        LocalDate lostUntil = candidate.lossReport().lostUntil();

        if (ChronoUnit.DAYS.between(foundOn, lostFrom) > weights.toleranceDays()) {
            return 0;
        }
        long duration = ChronoUnit.DAYS.between(lostFrom, lostUntil);
        long afterEnd = Math.max(0, ChronoUnit.DAYS.between(lostUntil, foundOn));
        long days = Math.max(duration, afterEnd);

        if (days <= weights.recentDays()) {
            return weights.recentPoints();
        }
        return days <= weights.closeDays() ? weights.closePoints() : 0;
    }
}
