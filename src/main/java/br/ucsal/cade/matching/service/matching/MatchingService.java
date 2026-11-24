package br.ucsal.cade.matching.service.matching;

import br.ucsal.cade.founditems.api.candidate.MatchableFoundItem;
import br.ucsal.cade.founditems.api.candidate.MatchableFoundItems;
import br.ucsal.cade.lossreports.api.candidate.MatchableLossReport;
import br.ucsal.cade.lossreports.api.candidate.MatchableLossReports;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.matching.service.score.result.Compatibility;
import br.ucsal.cade.matching.service.score.CompatibilityScorer;
import br.ucsal.cade.matching.service.score.candidate.FoundItemProfile;
import br.ucsal.cade.matching.service.score.candidate.LossReportProfile;
import br.ucsal.cade.matching.service.score.candidate.MatchCandidate;
import br.ucsal.cade.matching.service.score.candidate.Place;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Cruza um objeto ou uma perda recém-cadastrados com os registros em aberto do outro lado (RF20). Só a mesma
 * categoria é consultada, porque categoria diferente zera a nota (RN07), e só as sugestões médias e altas são
 * gravadas (seção 6.3).
 *
 * <p>É uma ação do sistema: roda na própria transação, fora da requisição, e vai para o histórico com o autor
 * {@link AuditEvent#SYSTEM_ACTOR} (complemento da D10).
 */
@Service
public class MatchingService {

    static final String SUGGESTED_ACTION = "MATCH_SUGGESTED";

    private final MatchableFoundItems foundItems;
    private final MatchableLossReports lossReports;
    private final CompatibilityScorer scorer;
    private final MatchSuggestionRepository suggestions;
    private final EventPublisher events;
    private final Clock clock;

    MatchingService(MatchableFoundItems foundItems, MatchableLossReports lossReports, CompatibilityScorer scorer,
                    MatchSuggestionRepository suggestions, EventPublisher events, Clock clock) {
        this.foundItems = foundItems;
        this.lossReports = lossReports;
        this.scorer = scorer;
        this.suggestions = suggestions;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Cruza o objeto com as perdas em aberto da mesma categoria. Nada acontece se o objeto não puder mais ser
     * convocado.
     *
     * @return as sugestões gravadas agora; um par que já tinha sugestão não volta
     */
    @Transactional
    public List<MatchSuggestion> matchFoundItem(UUID foundItemId) {
        return foundItems.find(foundItemId)
                .map(item -> suggest(lossReports.findByCategory(item.categoryId()).stream()
                        .map(report -> new Pair(item, report))))
                .orElseGet(List::of);
    }

    /**
     * Cruza a perda com os objetos que ainda podem ser convocados na mesma categoria. Nada acontece se o registro não
     * estiver mais em busca.
     *
     * @return as sugestões gravadas agora; um par que já tinha sugestão não volta
     */
    @Transactional
    public List<MatchSuggestion> matchLossReport(UUID lossReportId) {
        return lossReports.find(lossReportId)
                .map(report -> suggest(foundItems.findByCategory(report.categoryId()).stream()
                        .map(item -> new Pair(item, report))))
                .orElseGet(List::of);
    }

    private List<MatchSuggestion> suggest(Stream<Pair> pairs) {
        Instant now = clock.instant();
        return pairs
                .map(pair -> new ScoredPair(pair, scorer.score(pair.candidate())))
                .filter(scored -> scored.compatibility().band().isShownByDefault())
                .flatMap(scored -> save(scored, now).stream())
                .toList();
    }

    /** Grava a sugestão e o histórico; vazio se o par já tinha sugestão. */
    private Optional<MatchSuggestion> save(ScoredPair scored, Instant now) {
        Compatibility compatibility = scored.compatibility();
        MatchSuggestion suggestion = MatchSuggestion.suggest(scored.pair().item().id(), scored.pair().report().id(),
                compatibility.score(), now);
        if (!suggestions.saveIfAbsent(suggestion)) {
            return Optional.empty();
        }
        events.publish(new AuditEvent(AuditEvent.SYSTEM_ACTOR, SUGGESTED_ACTION,
                MatchSuggestion.class.getSimpleName(), suggestion.getId(), "Sugestão criada pelo cruzamento: "
                + "compatibilidade " + compatibility.score() + " (" + compatibility.band().label() + ")."));
        return Optional.of(suggestion);
    }

    private record Pair(MatchableFoundItem item, MatchableLossReport report) {

        MatchCandidate candidate() {
            FoundItemProfile found = new FoundItemProfile(item.categoryId(), item.categoryName(), item.foundOn(),
                    new Place(item.block(), item.room()), item.internalDescription());
            List<Place> places = report.locations().stream()
                    .map(location -> new Place(location.block(), location.room()))
                    .toList();
            LossReportProfile lost = new LossReportProfile(report.categoryId(), report.lostFrom(), report.lostUntil(),
                    places, report.description());
            return new MatchCandidate(found, lost);
        }
    }

    private record ScoredPair(Pair pair, Compatibility compatibility) {
    }
}
