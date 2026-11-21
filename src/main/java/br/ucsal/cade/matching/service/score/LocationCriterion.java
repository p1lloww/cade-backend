package br.ucsal.cade.matching.service.score;

/**
 * Mesma sala ou mesmo bloco (RN07). Vale o local da perda que mais pontua, e sala não informada no mesmo bloco conta
 * como mesmo bloco (D22). Bloco e sala são comparados sem caixa, acento e espaços extras.
 */
public record LocationCriterion(MatchingProperties.Location weights) implements CompatibilityCriterion {

    @Override
    public int score(MatchCandidate candidate) {
        Place found = candidate.foundItem().place();
        return candidate.lossReport().places().stream()
                .mapToInt(lost -> score(found, lost))
                .max()
                .orElse(0);
    }

    private int score(Place found, Place lost) {
        if (!Text.normalize(found.block()).equals(Text.normalize(lost.block()))) {
            return 0;
        }
        boolean sameRoom = found.room() != null && lost.room() != null
                && Text.normalize(found.room()).equals(Text.normalize(lost.room()));
        return sameRoom ? weights.sameRoomPoints() : weights.sameBlockPoints();
    }
}
