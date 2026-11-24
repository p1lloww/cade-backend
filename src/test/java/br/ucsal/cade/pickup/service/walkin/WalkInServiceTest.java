package br.ucsal.cade.pickup.service.walkin;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.pickup.api.event.WalkInPickupRegisteredEvent;
import br.ucsal.cade.pickup.exception.EnrollmentRequiredException;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.pickup.persistence.walkin.HolderType;
import br.ucsal.cade.pickup.persistence.walkin.WalkInHolder;
import br.ucsal.cade.pickup.persistence.walkin.WalkInPickup;
import br.ucsal.cade.pickup.persistence.walkin.WalkInPickupRepository;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.pickup.exception.VerificationNotAcceptedException;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;
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
 * Os objetos ficam numa categoria criada só para cada teste, sem perdas, para que o cruzamento automático disparado
 * pelos cadastros não grave sugestões além das que o teste cria.
 */
@SpringBootTest
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class WalkInServiceTest {

    @Autowired
    private WalkInService service;

    @Autowired
    private WalkInPickupRepository pickups;

    @Autowired
    private SummonsService summonsService;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private FoundItemRepository foundItems;

    @Autowired
    private LossReportService lossReportService;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    private UUID lowRisk;
    private UUID highRisk;
    private UUID staff;

    @BeforeEach
    void setUp() {
        lowRisk = newCategory(RiskLevel.LOW);
        highRisk = newCategory(RiskLevel.HIGH);
        staff = UUID.randomUUID();
    }

    @Test
    void showsWhatToCheckForTheRiskOfTheItem() {
        UUID item = find(highRisk);

        WalkInAtCounter atCounter = service.check(item);

        assertThat(atCounter.item().risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(atCounter.item().handOverWithoutSummons()).isTrue();
        assertThat(atCounter.rule().acceptedMethods())
                .containsExactlyInAnyOrder(VerificationMethod.DEVICE_UNLOCKED, VerificationMethod.PROOF_OF_OWNERSHIP);
    }

    @Test
    void aStudentPicksUpAnItemWithoutAReportAndTheOtherSuggestionsAreClosed() {
        UUID item = find(lowRisk);
        MatchSuggestion fresh = suggest(item);
        MatchSuggestion discarded = suggest(item);
        jdbc.update("UPDATE match_suggestions SET status = 'DISCARDED' WHERE id = ?", discarded.getId());

        RegisteredWalkIn registered = service.register(item, student(),
                check("202600001", VerificationMethod.DESCRIPTION_MATCHED), staff);

        WalkInPickup saved = pickups.findById(registered.pickup().getId()).orElseThrow();
        assertThat(saved.getFoundItemId()).isEqualTo(item);
        assertThat(saved.getHolder()).isEqualTo(student());
        assertThat(saved.getCheck().enrollment()).isEqualTo("202600001");
        assertThat(saved.getHandedOverBy()).isEqualTo(staff);
        assertThat(saved.getHandedOverAt()).isEqualTo(clock.instant());
        assertThat(itemStatus(item)).isEqualTo(FoundItemStatus.RETURNED);
        assertThat(registered.closedSuggestions()).containsExactly(fresh.getId());
        assertThat(suggestionStatus(fresh)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(suggestionStatus(discarded)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(events.stream(WalkInPickupRegisteredEvent.class)).containsExactly(
                new WalkInPickupRegisteredEvent(saved.getId(), item, List.of(fresh.getId())));
    }

    @Test
    void aPersonWithoutAccountIsIdentifiedByNameAndDocumentWithoutEnrollment() {
        UUID item = find(lowRisk);
        WalkInHolder visitor = new WalkInHolder(HolderType.VISITOR, "Pessoa Visitante");

        RegisteredWalkIn registered = service.register(item, visitor,
                check("999", VerificationMethod.DESCRIPTION_MATCHED), staff);

        WalkInPickup saved = pickups.findById(registered.pickup().getId()).orElseThrow();
        assertThat(saved.getHolder()).isEqualTo(visitor);
        assertThat(saved.getCheck().enrollment()).isNull();
        assertThat(saved.getCheck().documentNumber()).isEqualTo("000000");
    }

    @Test
    void everyChangeGoesToTheHistoryWithTheStaffAsAuthor() {
        UUID item = find(lowRisk);
        MatchSuggestion fresh = suggest(item);

        RegisteredWalkIn registered = service.register(item, student(),
                check("202600001", VerificationMethod.DESCRIPTION_MATCHED), staff);

        assertThat(jdbc.queryForList("""
                SELECT entity_type || ' ' || action FROM audit_records
                WHERE actor_id = ? AND entity_id IN (?, ?, ?)""", String.class, staff, item, fresh.getId(),
                registered.pickup().getId())).containsExactlyInAnyOrder(
                "FoundItem FOUND_ITEM_HANDED_OVER_AT_COUNTER", "MatchSuggestion MATCH_CLOSED",
                "WalkInPickup WALK_IN_PICKUP_REGISTERED");
    }

    @Test
    void aVerificationThatIsNotEnoughForTheRiskChangesNothing() {
        UUID item = find(highRisk);
        MatchSuggestion fresh = suggest(item);

        assertThatThrownBy(() -> service.register(item, student(),
                check("202600001", VerificationMethod.DESCRIPTION_MATCHED), staff))
                .isInstanceOf(VerificationNotAcceptedException.class)
                .isInstanceOf(BadRequestException.class);

        assertThat(itemStatus(item)).isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(suggestionStatus(fresh)).isEqualTo(SuggestionStatus.NEW);
        assertThat(pickupsOf(item)).isZero();
    }

    @Test
    void aStudentWithoutEnrollmentIsRefusedAndNothingChanges() {
        UUID item = find(lowRisk);

        assertThatThrownBy(() -> service.register(item, student(),
                check(" ", VerificationMethod.DESCRIPTION_MATCHED), staff))
                .isInstanceOf(EnrollmentRequiredException.class)
                .isInstanceOf(BadRequestException.class)
                .hasMessageStartingWith("Informe a matrícula");

        assertThat(itemStatus(item)).isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(pickupsOf(item)).isZero();
    }

    @Test
    void anItemReservedForASummonedStudentDoesNotLeaveThroughTheShortcut() {
        UUID item = find(lowRisk);
        summonsService.summon(suggest(item, lose()).getId(), staff);

        assertThatThrownBy(() -> service.register(item, student(),
                check("202600001", VerificationMethod.DESCRIPTION_MATCHED), staff))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .isInstanceOf(ConflictException.class);

        assertThat(itemStatus(item)).isEqualTo(FoundItemStatus.RESERVED);
        assertThat(pickupsOf(item)).isZero();
    }

    @Test
    void anItemHandedOverCannotLeaveTwice() {
        UUID item = find(lowRisk);
        service.register(item, student(), check("202600001", VerificationMethod.DESCRIPTION_MATCHED), staff);

        assertThatThrownBy(() -> service.register(item, student(),
                check("202600002", VerificationMethod.DESCRIPTION_MATCHED), staff))
                .isInstanceOf(ConflictException.class);
        assertThat(pickupsOf(item)).isOne();
    }

    @Test
    void anUnknownItemIsNotFound() {
        assertThatThrownBy(() -> service.register(UUID.randomUUID(), student(),
                check("202600001", VerificationMethod.DESCRIPTION_MATCHED), staff))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void aSimultaneousShortcutAndSummonsOfTheSameItemLetOnlyOnePass() throws Exception {
        UUID item = find(lowRisk);
        MatchSuggestion suggestion = suggest(item, lose());

        List<Throwable> failures = atTheSameTime(List.of(
                () -> service.register(item, student(), check("202600001", VerificationMethod.DESCRIPTION_MATCHED),
                        staff),
                () -> summonsService.summon(suggestion.getId(), staff)));

        assertThat(failures).hasSize(1).allSatisfy(failure -> assertThat(failure)
                .isInstanceOfAny(ConflictException.class, ObjectOptimisticLockingFailureException.class));
        FoundItemStatus status = itemStatus(item);
        if (status == FoundItemStatus.RETURNED) {
            assertThat(pickupsOf(item)).isOne();
            assertThat(suggestionStatus(suggestion)).isEqualTo(SuggestionStatus.CLOSED);
        } else {
            assertThat(status).isEqualTo(FoundItemStatus.RESERVED);
            assertThat(pickupsOf(item)).isZero();
            assertThat(suggestionStatus(suggestion)).isEqualTo(SuggestionStatus.SUMMONED);
        }
    }

    private static WalkInHolder student() {
        return new WalkInHolder(HolderType.STUDENT, "Estudante de Teste");
    }

    private static CounterCheck check(String enrollment, VerificationMethod verification) {
        return new CounterCheck(IdDocumentType.RG, "000000", enrollment, verification, null);
    }

    private long pickupsOf(UUID item) {
        return jdbc.queryForObject("SELECT count(*) FROM walk_in_pickups WHERE found_item_id = ?", Long.class, item);
    }

    private FoundItemStatus itemStatus(UUID item) {
        return foundItems.findById(item).orElseThrow().getStatus();
    }

    private SuggestionStatus suggestionStatus(MatchSuggestion suggestion) {
        return suggestions.findById(suggestion.getId()).orElseThrow().getStatus();
    }

    private MatchSuggestion suggest(UUID item) {
        return suggest(item, UUID.randomUUID());
    }

    private MatchSuggestion suggest(UUID item, UUID report) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, 70, clock.instant()));
    }

    private UUID find(UUID category) {
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 300", "Sala 305",
                LocalDate.now(clock), null, null, "Garrafa azul", null), UUID.randomUUID()).getId();
    }

    /** Numa categoria só desta perda, para o cruzamento não juntá-la aos objetos do teste. */
    private UUID lose() {
        return lossReportService.register(new RegisterLossReport(newCategory(RiskLevel.LOW), "Garrafa azul", null,
                List.of(), LocalDate.now(clock), null, null, null), UUID.randomUUID()).getId();
    }

    private UUID newCategory(RiskLevel risk) {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), risk)).getId();
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
