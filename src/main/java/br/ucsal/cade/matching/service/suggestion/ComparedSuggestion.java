package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.founditems.api.candidate.ComparableFoundItem;
import br.ucsal.cade.lossreports.api.candidate.ComparableLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.service.score.result.CompatibilityBand;

/** A sugestão com o objeto e a perda lado a lado, como o funcionário decide (RF22, RF23). */
public record ComparedSuggestion(MatchSuggestion suggestion, CompatibilityBand band, ComparableFoundItem foundItem,
                                 ComparableLossReport lossReport) {
}
