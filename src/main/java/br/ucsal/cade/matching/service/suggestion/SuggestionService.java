package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.matching.exception.SuggestionNotFoundException;

import br.ucsal.cade.founditems.api.candidate.ComparableFoundItem;
import br.ucsal.cade.founditems.api.candidate.ComparableFoundItems;
import br.ucsal.cade.lossreports.api.candidate.ComparableLossReport;
import br.ucsal.cade.lossreports.api.candidate.ComparableLossReports;
import br.ucsal.cade.lossreports.api.LossReportInfoRequests;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.matching.service.score.CompatibilityScorer;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * As sugestões do cruzamento diante do funcionário: listar a partir do objeto ou da perda e descartar (RF22, RF24).
 * A sugestão é apoio; quem decide é o funcionário (RN08).
 */
@Service
public class SuggestionService {

    static final String DISCARDED_ACTION = "MATCH_DISCARDED";

    private static final Logger log = LoggerFactory.getLogger(SuggestionService.class);

    private final MatchSuggestionRepository suggestions;
    private final ComparableFoundItems foundItems;
    private final ComparableLossReports lossReports;
    private final CompatibilityScorer scorer;
    private final LossReportInfoRequests infoRequests;
    private final EventPublisher events;

    SuggestionService(MatchSuggestionRepository suggestions, ComparableFoundItems foundItems,
                      ComparableLossReports lossReports, CompatibilityScorer scorer,
                      LossReportInfoRequests infoRequests, EventPublisher events) {
        this.suggestions = suggestions;
        this.foundItems = foundItems;
        this.lossReports = lossReports;
        this.scorer = scorer;
        this.infoRequests = infoRequests;
        this.events = events;
    }

    /** Só as sugestões ativas (Nova, Em espera, Convocada), da maior nota para a menor. */
    @Transactional(readOnly = true)
    public List<ComparedSuggestion> forFoundItem(UUID foundItemId) {
        log.debug("Listing suggestions of found item {}", foundItemId);
        return compare(suggestions.findByFoundItemIdAndStatusInOrderByScoreDescCreatedAtAsc(foundItemId,
                SuggestionStatus.ACTIVE));
    }

    /** Só as sugestões ativas (Nova, Em espera, Convocada), da maior nota para a menor. */
    @Transactional(readOnly = true)
    public List<ComparedSuggestion> forLossReport(UUID lossReportId) {
        log.debug("Listing suggestions of loss report {}", lossReportId);
        return compare(suggestions.findByLossReportIdAndStatusInOrderByScoreDescCreatedAtAsc(lossReportId,
                SuggestionStatus.ACTIVE));
    }

    /** Em qualquer situação, inclusive as finais. */
    @Transactional(readOnly = true)
    public ComparedSuggestion find(UUID suggestionId) {
        return compareOne(load(suggestionId));
    }

    /**
     * Nova → Descartada, com o motivo no histórico na mesma transação (RF24, RF43). Uma decisão simultânea sobre a
     * mesma sugestão perde no {@code @Version} e responde 409.
     *
     * @param staffId id do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public ComparedSuggestion discard(UUID suggestionId, String reason, UUID staffId) {
        MatchSuggestion suggestion = load(suggestionId);
        try {
            suggestion.discard();
        } catch (InvalidStatusTransitionException e) {
            log.info("Discard refused for suggestion {}: status is {}", suggestionId, suggestion.getStatus().code());
            throw e;
        }
        events.publish(new AuditEvent(staffId, DISCARDED_ACTION, MatchSuggestion.class.getSimpleName(),
                suggestion.getId(), reason));
        log.info("Suggestion {} discarded by {}", suggestionId, staffId);
        return compareOne(suggestion);
    }

    /**
     * Pede mais informações ao estudante a partir da sugestão (RF24): o registro de perda passa a Aguardando
     * informações, com o motivo no histórico e no e-mail ao estudante (RF35). A sugestão continua Nova; se o estudante
     * não responder em 5 dias, ela é descartada (RN13). Só uma sugestão Nova admite o pedido.
     *
     * @param staffId id do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public ComparedSuggestion requestInfo(UUID suggestionId, String reason, UUID staffId) {
        MatchSuggestion suggestion = load(suggestionId);
        if (suggestion.getStatus() != SuggestionStatus.NEW) {
            log.info("Info request refused for suggestion {}: status is {}", suggestionId,
                    suggestion.getStatus().code());
            throw new InvalidStatusTransitionException("pedir informações sobre", "uma sugestão",
                    suggestion.getStatus().label());
        }
        infoRequests.request(suggestion.getLossReportId(), suggestion.getId(), reason, staffId);
        return compareOne(suggestion);
    }

    private MatchSuggestion load(UUID suggestionId) {
        return suggestions.findById(suggestionId).orElseThrow(() -> {
            log.info("Suggestion {} not found", suggestionId);
            return new SuggestionNotFoundException();
        });
    }

    private ComparedSuggestion compareOne(MatchSuggestion suggestion) {
        return compare(List.of(suggestion)).getFirst();
    }

    /** Busca os objetos e as perdas de todas as sugestões em duas consultas, não em uma por sugestão. */
    private List<ComparedSuggestion> compare(List<MatchSuggestion> found) {
        Map<UUID, ComparableFoundItem> items = foundItems.findAll(
                found.stream().map(MatchSuggestion::getFoundItemId).distinct().toList());
        Map<UUID, ComparableLossReport> reports = lossReports.findAll(
                found.stream().map(MatchSuggestion::getLossReportId).distinct().toList());
        return found.stream()
                .map(suggestion -> new ComparedSuggestion(suggestion, scorer.bandOf(suggestion.getScore()),
                        items.get(suggestion.getFoundItemId()), reports.get(suggestion.getLossReportId())))
                .toList();
    }
}
