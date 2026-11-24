package br.ucsal.cade.pickup.service.summons;

import br.ucsal.cade.founditems.api.reservation.FoundItemReservations;
import br.ucsal.cade.founditems.api.reservation.ReservedFoundItem;
import br.ucsal.cade.lossreports.api.summons.LossReportSummons;
import br.ucsal.cade.lossreports.api.summons.SummonedLossReport;
import br.ucsal.cade.matching.api.SuggestionPair;
import br.ucsal.cade.matching.api.SuggestionSummons;
import br.ucsal.cade.matching.api.SummonedSuggestion;
import br.ucsal.cade.pickup.api.ExpiringSummons;
import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.pickup.api.SummonsDeadlines;
import br.ucsal.cade.pickup.api.event.SummonsCreatedEvent;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.NewSummons;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.pickup.service.verification.VerificationRules;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.event.EventPublisher;
import br.ucsal.cade.shared.settings.Deadlines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Convocação a partir de uma sugestão (RF26): o objeto fica Reservado, a perda Convocado e a sugestão Convocada, na
 * mesma transação (seção 7.1), com cada mudança no histórico (RF43).
 *
 * <p>Um objeto tem no máximo uma convocação ativa (RN18). Duas convocações simultâneas do mesmo objeto, pela mesma
 * sugestão ou por sugestões diferentes, disputam a linha do objeto: a segunda perde no {@code @Version} e responde
 * 409, sem mudar nada. Detalhes em {@code docs/tecnico/convocacao.md}.
 */
@Service
public class SummonsService implements SummonsDeadlines {

    static final String CREATED_ACTION = "SUMMONS_CREATED";

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Logger log = LoggerFactory.getLogger(SummonsService.class);

    private final SummonsRepository repository;
    private final SuggestionSummons suggestions;
    private final FoundItemReservations foundItems;
    private final LossReportSummons lossReports;
    private final PickupProperties properties;
    private final Deadlines deadlines;
    private final VerificationRules rules;
    private final EventPublisher events;
    private final Clock clock;

    SummonsService(SummonsRepository repository, SuggestionSummons suggestions, FoundItemReservations foundItems,
                   LossReportSummons lossReports, PickupProperties properties, Deadlines deadlines,
                   VerificationRules rules, EventPublisher events, Clock clock) {
        this.repository = repository;
        this.suggestions = suggestions;
        this.foundItems = foundItems;
        this.lossReports = lossReports;
        this.properties = properties;
        this.deadlines = deadlines;
        this.rules = rules;
        this.events = events;
        this.clock = clock;
    }

    /**
     * O objeto é reservado primeiro, e só depois as sugestões e a perda mudam: assim toda convocação do mesmo objeto
     * disputa a mesma linha na mesma ordem, e a que chega depois perde no {@code @Version} em vez de travar.
     *
     * @param staffId id do funcionário (claim {@code sub} do token)
     */
    @Transactional
    public IssuedSummons summon(UUID suggestionId, UUID staffId) {
        log.debug("Summoning from suggestion {} by {}", suggestionId, staffId);
        SuggestionPair pair = suggestions.pairOf(suggestionId);
        ReservedFoundItem item = foundItems.reserve(pair.foundItemId(), staffId);
        SummonedSuggestion summoned = suggestions.summon(suggestionId, staffId);
        SummonedLossReport report = lossReports.summon(pair.lossReportId(), staffId);

        Instant now = clock.instant();
        Summons summons = repository.save(Summons.issue(new NewSummons(pair.suggestionId(), pair.foundItemId(),
                pair.lossReportId(), report.studentId(), staffId, item.risk()), now, now.plus(deadlines.summonsPeriod())));
        PickupInstructions instructions = instructionsFor(summons);

        events.publish(new AuditEvent(staffId, CREATED_ACTION, Summons.class.getSimpleName(), summons.getId(),
                "Estudante convocado; o objeto fica reservado até "
                        + DEADLINE_FORMAT.format(summons.getExpiresAt().atZone(clock.getZone())) + "."));
        events.publish(new SummonsCreatedEvent(summons.getId(), summons.getStudentId(), summons.getFoundItemId(),
                summons.getLossReportId(), instructions));
        logOutcomeAfterCommit(summons, summoned);
        return new IssuedSummons(summons, instructions, summoned.onHold());
    }

    /**
     * As convocações Ativas do estudante, com onde, até quando e o que levar (RF16, RF26), as que vencem primeiro no
     * topo. Concluída, Recusada e Vencida saem da lista: aí a situação do registro de perda já conta o que houve.
     */
    @Transactional(readOnly = true)
    public List<StudentSummons> activeOf(UUID studentId) {
        List<StudentSummons> active = repository.findByStudentIdAndStatusOrderByExpiresAtAsc(studentId,
                        SummonsStatus.ACTIVE).stream()
                .map(summons -> new StudentSummons(summons, instructionsFor(summons)))
                .toList();
        log.debug("Listed {} active summons of student {}", active.size(), studentId);
        return active;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringSummons> expiringBetween(Instant from, Instant to) {
        return repository.findByStatusAndExpiresAtGreaterThanAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
                        SummonsStatus.ACTIVE, from, to).stream()
                .map(summons -> new ExpiringSummons(summons.getId(), summons.getStudentId(), instructionsFor(summons)))
                .toList();
    }

    private PickupInstructions instructionsFor(Summons summons) {
        return new PickupInstructions(properties.place(), properties.hours(), summons.getExpiresAt(),
                rules.forRisk(summons.getRisk()).whatToBring());
    }

    private void logOutcomeAfterCommit(Summons summons, SummonedSuggestion summoned) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    log.info("Summons {} issued: suggestion={}, item={}, report={}, onHold={}", summons.getId(),
                            summons.getSuggestionId(), summons.getFoundItemId(), summons.getLossReportId(),
                            summoned.onHold().size());
                } else {
                    log.warn("Summons from suggestion {} rolled back at commit (probably a simultaneous summons)",
                            summons.getSuggestionId());
                }
            }
        });
    }
}
