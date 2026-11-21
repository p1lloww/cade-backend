package br.ucsal.cade.pickup.service.counter;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.lossreports.persistence.status.LossReportStatus;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.lossreports.service.report.StudentBlockedException;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.pickup.api.event.PickupRegisteredEvent;
import br.ucsal.cade.pickup.exception.SummonsNotFoundException;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import br.ucsal.cade.pickup.persistence.pickup.PickupRepository;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.pickup.service.verification.VerificationNotAcceptedException;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os objetos e as perdas ficam em categorias diferentes, criadas só para cada teste, para que o cruzamento automático
 * disparado pelos cadastros não grave sugestões além das que o teste cria.
 */
@SpringBootTest
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CounterServiceTest {

    @Autowired
    private CounterService service;

    @Autowired
    private SummonsService summonsService;

    @Autowired
    private SummonsRepository summonses;

    @Autowired
    private PickupRepository pickups;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private FoundItemRepository foundItems;

    @Autowired
    private LossReportService lossReportService;

    @Autowired
    private LossReportRepository lossReports;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    private UUID itemCategory;
    private UUID reportCategory;
    private UUID student;
    private UUID staff;

    @BeforeEach
    void setUp() {
        itemCategory = newCategory(RiskLevel.MEDIUM);
        reportCategory = newCategory(RiskLevel.MEDIUM);
        student = UUID.randomUUID();
        staff = UUID.randomUUID();
    }

    @Test
    void theCounterSeesWhatToCheckForTheRiskOfTheItem() {
        Summons summons = summon(find(newCategory(RiskLevel.HIGH)), lose(student));

        SummonsAtCounter atCounter = service.find(summons.getId());

        assertThat(atCounter.rule().risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(atCounter.rule().acceptedMethods()).containsExactlyInAnyOrder(VerificationMethod.DEVICE_UNLOCKED,
                VerificationMethod.PROOF_OF_OWNERSHIP);
    }

    @Test
    void anUnknownSummonsIsNotFound() {
        assertThatThrownBy(() -> service.find(UUID.randomUUID())).isInstanceOf(SummonsNotFoundException.class);
    }

    @Test
    void theActiveListShowsFirstTheSummonsThatExpiresFirst() {
        Summons first = summon(find(), lose());
        clock.advance(Duration.ofHours(1));
        Summons second = summon(find(), lose());
        Summons refused = summon(find(), lose());
        service.refuse(refused.getId(), "Não soube descrever o detalhe.", staff);

        List<UUID> active = service.active().stream().map(atCounter -> atCounter.summons().getId()).toList();

        assertThat(active).containsSubsequence(first.getId(), second.getId()).doesNotContain(refused.getId());
    }

    @Test
    void aPickupHandsOverTheItemAndCompletesTheSummonsTheReportAndTheSuggestion() {
        Summons summons = summon(find(), lose(student));

        Pickup pickup = service.registerPickup(summons.getId(), check(VerificationMethod.DETAIL_CONFIRMED), staff);

        Pickup saved = pickups.findById(pickup.getId()).orElseThrow();
        assertThat(saved.getSummonsId()).isEqualTo(summons.getId());
        assertThat(saved.getStudentId()).isEqualTo(student);
        assertThat(saved.getFoundItemId()).isEqualTo(summons.getFoundItemId());
        assertThat(saved.getCheck()).isEqualTo(check(VerificationMethod.DETAIL_CONFIRMED));
        assertThat(saved.getHandedOverBy()).isEqualTo(staff);
        assertThat(saved.getHandedOverAt()).isEqualTo(clock.instant());

        Summons completed = summonses.findById(summons.getId()).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(SummonsStatus.COMPLETED);
        assertThat(completed.getClosedBy()).isEqualTo(staff);
        assertThat(completed.getClosedAt()).isEqualTo(clock.instant());
        assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.RETURNED);
        assertThat(reportStatus(summons)).isEqualTo(LossReportStatus.COMPLETED);
        assertThat(suggestionStatus(summons.getSuggestionId())).isEqualTo(SuggestionStatus.COMPLETED);
    }

    @Test
    void everyChangeOfThePickupGoesToTheHistoryWithTheStaffAsAuthor() {
        Summons summons = summon(find(), lose());

        Pickup pickup = service.registerPickup(summons.getId(), check(VerificationMethod.DETAIL_CONFIRMED), staff);

        assertThat(history(summons, pickup.getId())).containsExactlyInAnyOrder("Summons SUMMONS_COMPLETED",
                "Pickup PICKUP_REGISTERED", "FoundItem FOUND_ITEM_HANDED_OVER", "LossReport LOSS_REPORT_COMPLETED",
                "MatchSuggestion MATCH_COMPLETED");
    }

    @Test
    void publishesThePickupRegisteredEvent() {
        Summons summons = summon(find(), lose(student));

        Pickup pickup = service.registerPickup(summons.getId(), check(VerificationMethod.DETAIL_CONFIRMED), staff);

        assertThat(events.stream(PickupRegisteredEvent.class)).containsExactly(new PickupRegisteredEvent(
                pickup.getId(), summons.getId(), student, summons.getFoundItemId(), summons.getLossReportId()));
    }

    @Test
    void aHighRiskItemIsHandedOverWithAProofOfOwnership() {
        Summons summons = summon(find(newCategory(RiskLevel.HIGH)), lose());

        service.registerPickup(summons.getId(), check(VerificationMethod.PROOF_OF_OWNERSHIP), staff);

        assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.RETURNED);
    }

    @Test
    void aVerificationThatIsNotEnoughForTheRiskChangesNothing() {
        Summons summons = summon(find(newCategory(RiskLevel.HIGH)), lose());

        assertThatThrownBy(() -> service.registerPickup(summons.getId(),
                check(VerificationMethod.DESCRIPTION_MATCHED), staff))
                .isInstanceOf(VerificationNotAcceptedException.class);

        assertThat(summonses.findById(summons.getId()).orElseThrow().getStatus()).isEqualTo(SummonsStatus.ACTIVE);
        assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.RESERVED);
        assertThat(pickupsOf(summons)).isZero();
        assertThat(events.stream(PickupRegisteredEvent.class)).isEmpty();
    }

    @Test
    void aRefusalReleasesTheItemAndEndsTheReportAndTheSuggestion() {
        Summons summons = summon(find(), lose(student));

        RefusedSummons refused = service.refuse(summons.getId(), "Não soube descrever o detalhe.", staff);

        Summons saved = summonses.findById(summons.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SummonsStatus.REFUSED);
        assertThat(saved.getRefusalReason()).isEqualTo("Não soube descrever o detalhe.");
        assertThat(saved.getClosedBy()).isEqualTo(staff);
        assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(reportStatus(summons)).isEqualTo(LossReportStatus.REFUSED);
        assertThat(suggestionStatus(summons.getSuggestionId())).isEqualTo(SuggestionStatus.REFUSED);
        assertThat(refused.refusal().refusals()).isOne();
        assertThat(refused.refusal().blocked()).isFalse();
    }

    @Test
    void theSuggestionsOnHoldOfTheItemComeBackAfterARefusal() {
        UUID item = find();
        MatchSuggestion best = suggest(item, lose(), 80);
        MatchSuggestion waiting = suggest(item, lose(), 50);
        Summons summons = summonsService.summon(best.getId(), staff).summons();
        assertThat(suggestionStatus(waiting.getId())).isEqualTo(SuggestionStatus.ON_HOLD);

        RefusedSummons refused = service.refuse(summons.getId(), "Não desbloqueou o aparelho.", staff);

        assertThat(refused.suggestion().resumed()).containsExactly(waiting.getId());
        assertThat(suggestionStatus(waiting.getId())).isEqualTo(SuggestionStatus.NEW);
    }

    @Test
    void theOtherSuggestionsOfTheRefusedReportAreClosed() {
        UUID report = lose(student);
        MatchSuggestion summoned = suggest(find(), report, 80);
        MatchSuggestion otherItem = suggest(find(), report, 60);
        MatchSuggestion otherReport = suggest(find(), lose(), 70);
        Summons summons = summonsService.summon(summoned.getId(), staff).summons();

        RefusedSummons refused = service.refuse(summons.getId(), "Verificação falhou.", staff);

        assertThat(refused.suggestion().closed()).containsExactly(otherItem.getId());
        assertThat(suggestionStatus(otherItem.getId())).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(suggestionStatus(otherReport.getId())).isEqualTo(SuggestionStatus.NEW);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE entity_id = ? AND action = ?",
                Long.class, otherItem.getId(), "MATCH_CLOSED")).isOne();
    }

    @Test
    void everyChangeOfTheRefusalGoesToTheHistoryWithTheStaffAsAuthor() {
        Summons summons = summon(find(), lose(student));

        service.refuse(summons.getId(), "Não soube descrever o detalhe.", staff);

        assertThat(history(summons, student)).containsExactlyInAnyOrder("Summons SUMMONS_REFUSED",
                "FoundItem FOUND_ITEM_RELEASED", "LossReport LOSS_REPORT_REFUSED", "MatchSuggestion MATCH_REFUSED",
                "LossReporter STUDENT_REFUSAL_RECORDED");
    }

    @Test
    void theThirdRefusalBlocksNewReportsOfTheStudent() {
        RefusedSummons last = null;
        for (int i = 0; i < 3; i++) {
            last = service.refuse(summon(find(), lose(student)).getId(), "Verificação falhou.", staff);
        }

        assertThat(last.refusal().refusals()).isEqualTo(3);
        assertThat(last.refusal().blocked()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE entity_id = ? AND action = ?",
                Long.class, student, "STUDENT_BLOCKED")).isOne();
        assertThatThrownBy(() -> lose(student)).isInstanceOf(StudentBlockedException.class);
    }

    @Test
    void aFinishedSummonsCannotBeHandedOverOrRefusedAgain() {
        Summons summons = summon(find(), lose());
        service.refuse(summons.getId(), "Verificação falhou.", staff);

        assertThatThrownBy(() -> service.registerPickup(summons.getId(),
                check(VerificationMethod.DETAIL_CONFIRMED), staff))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThatThrownBy(() -> service.refuse(summons.getId(), "De novo.", staff))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.AVAILABLE);
    }

    @Test
    void aSimultaneousPickupAndRefusalLetOnlyOnePass() throws Exception {
        Summons summons = summon(find(), lose(student));

        List<Throwable> failures = atTheSameTime(List.of(
                () -> service.registerPickup(summons.getId(), check(VerificationMethod.DETAIL_CONFIRMED), staff),
                () -> service.refuse(summons.getId(), "Verificação falhou.", staff)));

        assertThat(failures).hasSize(1).allSatisfy(failure -> assertThat(failure)
                .isInstanceOfAny(ConflictException.class, ObjectOptimisticLockingFailureException.class));
        SummonsStatus status = summonses.findById(summons.getId()).orElseThrow().getStatus();
        if (status == SummonsStatus.COMPLETED) {
            assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.RETURNED);
            assertThat(refusalsOf(student)).isZero();
        } else {
            assertThat(status).isEqualTo(SummonsStatus.REFUSED);
            assertThat(itemStatus(summons)).isEqualTo(FoundItemStatus.AVAILABLE);
            assertThat(pickupsOf(summons)).isZero();
        }
    }

    private List<String> history(Summons summons, UUID extra) {
        return jdbc.queryForList("""
                SELECT entity_type || ' ' || action FROM audit_records
                WHERE actor_id = ? AND entity_id IN (?, ?, ?, ?, ?)""", String.class, staff, summons.getId(),
                summons.getFoundItemId(), summons.getLossReportId(), summons.getSuggestionId(), extra);
    }

    private static CounterCheck check(VerificationMethod verification) {
        return new CounterCheck(IdDocumentType.STUDENT_CARD, "000000", "000000000", verification, null);
    }

    private long pickupsOf(Summons summons) {
        return jdbc.queryForObject("SELECT count(*) FROM pickups WHERE summons_id = ?", Long.class, summons.getId());
    }

    private int refusalsOf(UUID owner) {
        return jdbc.queryForList("SELECT refusals FROM loss_reporters WHERE student_id = ?", Integer.class, owner)
                .stream().findFirst().orElse(0);
    }

    private FoundItemStatus itemStatus(Summons summons) {
        return foundItems.findById(summons.getFoundItemId()).orElseThrow().getStatus();
    }

    private LossReportStatus reportStatus(Summons summons) {
        return lossReports.findById(summons.getLossReportId()).orElseThrow().getStatus();
    }

    private SuggestionStatus suggestionStatus(UUID suggestionId) {
        return suggestions.findById(suggestionId).orElseThrow().getStatus();
    }

    private Summons summon(UUID item, UUID report) {
        return summonsService.summon(suggest(item, report, 80).getId(), UUID.randomUUID()).summons();
    }

    private MatchSuggestion suggest(UUID item, UUID report, int score) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, score, clock.instant()));
    }

    private UUID find() {
        return find(itemCategory);
    }

    private UUID find(UUID category) {
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                today(), null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    /** Cada perda de um estudante diferente, para não esbarrar no limite de 5 registros (RN05). */
    private UUID lose() {
        return lose(UUID.randomUUID());
    }

    private UUID lose(UUID owner) {
        return lossReportService.register(new RegisterLossReport(reportCategory, "Fone JBL preto",
                "Adesivo de estrela", List.of(LossLocation.of("Bloco 400", "Sala 421")), today(), null, null, null),
                owner).getId();
    }

    private UUID newCategory(RiskLevel risk) {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), risk)).getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private List<Throwable> atTheSameTime(List<Callable<?>> tasks) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> results = new ArrayList<>();
            for (Callable<?> task : tasks) {
                results.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            for (Future<?> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
        }
        return failures;
    }
}
