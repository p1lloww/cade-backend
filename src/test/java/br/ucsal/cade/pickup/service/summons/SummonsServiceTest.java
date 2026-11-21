package br.ucsal.cade.pickup.service.summons;

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
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.pickup.api.event.SummonsCreatedEvent;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
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
class SummonsServiceTest {

    @Autowired
    private SummonsService service;

    @Autowired
    private SummonsRepository summonses;

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

    @BeforeEach
    void setUp() {
        itemCategory = newCategory(RiskLevel.MEDIUM);
        reportCategory = newCategory(RiskLevel.MEDIUM);
        student = UUID.randomUUID();
    }

    @Test
    void summoningReservesTheItemAndSummonsTheReportAndTheSuggestion() {
        MatchSuggestion suggestion = suggest(find(), lose(student), 80);
        UUID staff = UUID.randomUUID();

        IssuedSummons issued = service.summon(suggestion.getId(), staff);

        Summons summons = summonses.findById(issued.summons().getId()).orElseThrow();
        assertThat(summons.getStatus()).isEqualTo(SummonsStatus.ACTIVE);
        assertThat(summons.getSuggestionId()).isEqualTo(suggestion.getId());
        assertThat(summons.getFoundItemId()).isEqualTo(suggestion.getFoundItemId());
        assertThat(summons.getLossReportId()).isEqualTo(suggestion.getLossReportId());
        assertThat(summons.getStudentId()).isEqualTo(student);
        assertThat(summons.getSummonedBy()).isEqualTo(staff);
        assertThat(summons.getRisk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(foundItems.findById(suggestion.getFoundItemId())).get()
                .extracting(item -> item.getStatus()).isEqualTo(FoundItemStatus.RESERVED);
        assertThat(lossReports.findById(suggestion.getLossReportId())).get()
                .extracting(report -> report.getStatus()).isEqualTo(LossReportStatus.SUMMONED);
        assertThat(suggestions.findById(suggestion.getId())).get()
                .extracting(MatchSuggestion::getStatus).isEqualTo(SuggestionStatus.SUMMONED);
    }

    @Test
    void theDescriptionOfTheReportIsLockedAfterTheSummons() {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);

        service.summon(suggestion.getId(), UUID.randomUUID());

        assertThat(lossReports.findById(suggestion.getLossReportId()).orElseThrow().getStatus().isEditable())
                .isFalse();
    }

    @Test
    void everyChangeGoesToTheHistoryWithTheStaffAsAuthor() {
        UUID item = find();
        MatchSuggestion summoned = suggest(item, lose(), 80);
        MatchSuggestion other = suggest(item, lose(), 50);
        UUID staff = UUID.randomUUID();

        IssuedSummons issued = service.summon(summoned.getId(), staff);

        assertThat(jdbc.queryForList("""
                SELECT entity_type || ' ' || action FROM audit_records
                WHERE actor_id = ? AND entity_id IN (?, ?, ?, ?, ?)""", String.class, staff,
                issued.summons().getId(), item, summoned.getLossReportId(), summoned.getId(), other.getId()))
                .containsExactlyInAnyOrder("Summons SUMMONS_CREATED", "FoundItem FOUND_ITEM_RESERVED",
                        "LossReport LOSS_REPORT_SUMMONED", "MatchSuggestion MATCH_SUMMONED",
                        "MatchSuggestion MATCH_ON_HOLD");
    }

    @Test
    void theOtherNewSuggestionsOfTheItemGoOnHold() {
        UUID item = find();
        MatchSuggestion best = suggest(item, lose(), 80);
        MatchSuggestion other = suggest(item, lose(), 50);
        MatchSuggestion discarded = suggest(item, lose(), 45);
        discarded.discard();
        suggestions.saveAndFlush(discarded);
        MatchSuggestion otherItem = suggest(find(), other.getLossReportId(), 60);

        IssuedSummons issued = service.summon(best.getId(), UUID.randomUUID());

        assertThat(issued.onHold()).containsExactly(other.getId());
        assertThat(statusOf(other)).isEqualTo(SuggestionStatus.ON_HOLD);
        assertThat(statusOf(discarded)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(statusOf(otherItem)).isEqualTo(SuggestionStatus.NEW);
        assertThat(lossReports.findById(other.getLossReportId()).orElseThrow().getStatus())
                .isEqualTo(LossReportStatus.OPEN);
    }

    @Test
    void theStudentGetsThePlaceHoursDeadlineAndWhatToBring() {
        MatchSuggestion suggestion = suggest(find(newCategory(RiskLevel.HIGH)), lose(), 80);

        IssuedSummons issued = service.summon(suggestion.getId(), UUID.randomUUID());

        assertThat(issued.summons().getSummonedAt()).isEqualTo(clock.instant());
        assertThat(issued.summons().getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofDays(7)));
        assertThat(issued.instructions().deadline()).isEqualTo(issued.summons().getExpiresAt());
        assertThat(issued.instructions().place()).isEqualTo("Sala de achados e perdidos da UCSal");
        assertThat(issued.instructions().hours()).isEqualTo("Das 7h às 21h50");
        assertThat(issued.instructions().whatToBring()).isEqualTo(WhatToBring.forRisk(RiskLevel.HIGH));
    }

    @Test
    void publishesTheSummonsCreatedEvent() {
        MatchSuggestion suggestion = suggest(find(), lose(student), 80);

        IssuedSummons issued = service.summon(suggestion.getId(), UUID.randomUUID());

        assertThat(events.stream(SummonsCreatedEvent.class)).containsExactly(new SummonsCreatedEvent(
                issued.summons().getId(), student, suggestion.getFoundItemId(), suggestion.getLossReportId(),
                issued.instructions()));
    }

    @Test
    void aSuggestionThatIsNotNewCannotBeSummonedAndNothingChanges() {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        suggestion.discard();
        suggestions.saveAndFlush(suggestion);

        assertThatThrownBy(() -> service.summon(suggestion.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Não é possível convocar uma sugestão na situação Descartada.");
        assertNothingChanged(suggestion);
        assertThat(events.stream(SummonsCreatedEvent.class)).isEmpty();
    }

    @Test
    void aReportAlreadySummonedForAnotherItemCannotBeSummonedAgain() {
        UUID report = lose();
        MatchSuggestion first = suggest(find(), report, 80);
        MatchSuggestion second = suggest(find(), report, 70);
        service.summon(first.getId(), UUID.randomUUID());

        assertThatThrownBy(() -> service.summon(second.getId(), UUID.randomUUID()))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Não é possível convocar um registro de perda na situação Convocado.");
        assertThat(foundItems.findById(second.getFoundItemId()).orElseThrow().getStatus())
                .isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(statusOf(second)).isEqualTo(SuggestionStatus.NEW);
    }

    @Test
    void anUnknownSuggestionIsNotFound() {
        assertThatThrownBy(() -> service.summon(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void simultaneousSummonsOfTheSameItemLetOnlyOnePass() throws Exception {
        UUID item = find();
        List<MatchSuggestion> competing = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            competing.add(suggest(item, lose(), 50 + i));
        }

        List<Throwable> failures = atTheSameTime(competing.stream()
                .<Callable<IssuedSummons>>map(suggestion -> () -> service.summon(suggestion.getId(),
                        UUID.randomUUID()))
                .toList());

        assertThat(failures).hasSize(5).allSatisfy(failure -> assertThat(failure)
                .isInstanceOfAny(ConflictException.class, ObjectOptimisticLockingFailureException.class));
        assertThat(summonsesOf(item)).isOne();
        assertThat(competing).extracting(this::statusOf).containsOnly(SuggestionStatus.SUMMONED,
                SuggestionStatus.ON_HOLD).filteredOn(SuggestionStatus.SUMMONED::equals).hasSize(1);
        assertThat(competing).extracting(suggestion -> lossReports.findById(suggestion.getLossReportId())
                .orElseThrow().getStatus()).filteredOn(LossReportStatus.SUMMONED::equals).hasSize(1);
    }

    @Test
    void simultaneousSummonsOfTheSameSuggestionLetOnlyOnePass() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        Callable<IssuedSummons> summon = () -> service.summon(suggestion.getId(), UUID.randomUUID());

        List<Throwable> failures = atTheSameTime(List.of(summon, summon, summon));

        assertThat(failures).hasSize(2).allSatisfy(failure -> assertThat(failure)
                .isInstanceOfAny(ConflictException.class, ObjectOptimisticLockingFailureException.class));
        assertThat(summonsesOf(suggestion.getFoundItemId())).isOne();
    }

    private void assertNothingChanged(MatchSuggestion suggestion) {
        assertThat(foundItems.findById(suggestion.getFoundItemId()).orElseThrow().getStatus())
                .isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(lossReports.findById(suggestion.getLossReportId()).orElseThrow().getStatus())
                .isEqualTo(LossReportStatus.OPEN);
        assertThat(summonsesOf(suggestion.getFoundItemId())).isZero();
    }

    private List<Throwable> atTheSameTime(List<Callable<IssuedSummons>> tasks) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> failures = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<IssuedSummons>> results = tasks.stream().map(task -> executor.submit(() -> {
                start.await();
                return task.call();
            })).toList();
            start.countDown();
            for (Future<IssuedSummons> result : results) {
                try {
                    result.get();
                } catch (ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
        }
        return failures;
    }

    private long summonsesOf(UUID foundItemId) {
        return jdbc.queryForObject("SELECT count(*) FROM summons WHERE found_item_id = ?", Long.class, foundItemId);
    }

    private SuggestionStatus statusOf(MatchSuggestion suggestion) {
        return suggestions.findById(suggestion.getId()).orElseThrow().getStatus();
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
}
