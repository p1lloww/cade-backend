package br.ucsal.cade.matching.api;

import java.util.Objects;
import java.util.UUID;

/** O par objeto × perda de uma sugestão. */
public record SuggestionPair(UUID suggestionId, UUID foundItemId, UUID lossReportId) {

    public SuggestionPair {
        Objects.requireNonNull(suggestionId);
        Objects.requireNonNull(foundItemId);
        Objects.requireNonNull(lossReportId);
    }
}
