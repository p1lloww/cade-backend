package br.ucsal.cade.matching.service.score;

import java.util.Objects;

/** O par objeto × perda que o cruzamento avalia. */
public record MatchCandidate(FoundItemProfile foundItem, LossReportProfile lossReport) {

    public MatchCandidate {
        Objects.requireNonNull(foundItem);
        Objects.requireNonNull(lossReport);
    }
}
