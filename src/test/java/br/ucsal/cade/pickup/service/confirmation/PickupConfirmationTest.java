package br.ucsal.cade.pickup.service.confirmation;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.pickup.api.event.OtherSuggestionsClosedEvent;
import br.ucsal.cade.pickup.exception.PickupAlreadyConfirmedException;
import br.ucsal.cade.pickup.exception.PickupNotFoundException;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import br.ucsal.cade.pickup.persistence.pickup.PickupConfirmationRepository;
import br.ucsal.cade.pickup.persistence.pickup.PickupRepository;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.service.counter.CounterService;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Com a retirada registrada no balcão, as outras sugestões do objeto são encerradas e os estudantes avisados (RF33), e
 * o estudante confirma a retirada no celular (RF30). Objetos e perdas ficam em categorias criadas só para cada teste,
 * para o cruzamento automático não gravar sugestões além das que o teste cria.
 */
@SpringBootTest
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PickupConfirmationTest {

    @Autowired
    private PickupConfirmationService confirmationService;

    @Autowired
    private CounterService counter;

    @Autowired
    private SummonsService summonsService;

    @Autowired
    private PickupRepository pickups;

    @Autowired
    private PickupConfirmationRepository confirmations;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private LossReportService lossReportService;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @MockitoBean
    private EmailSender emailSender;

    private UUID itemCategory;
    private UUID reportCategory;

    @BeforeEach
    void setUp() {
        when(emailSender.send(any())).thenReturn(true);
        itemCategory = newCategory();
        reportCategory = newCategory();
    }

    // ---- RF33: encerrar as outras sugestões do objeto

    @Test
    void thePickupClosesTheOtherSuggestionsOfTheItemAndLeavesTheOnesOfOtherItemsAlone() {
        UUID item = find();
        MatchSuggestion winner = suggest(item, lose(UUID.randomUUID()));
        MatchSuggestion fresh = suggest(item, lose(UUID.randomUUID()));
        MatchSuggestion discarded = suggest(item, lose(UUID.randomUUID()));
        discarded.discard();
        suggestions.saveAndFlush(discarded);
        MatchSuggestion otherItem = suggest(find(), lose(UUID.randomUUID()));
        Summons summons = summon(winner);
        UUID staff = UUID.randomUUID();
        assertThat(statusOf(fresh)).isEqualTo(SuggestionStatus.ON_HOLD);

        counter.registerPickup(summons.getId(), check(), staff);

        assertThat(statusOf(winner)).isEqualTo(SuggestionStatus.COMPLETED);
        assertThat(statusOf(fresh)).isEqualTo(SuggestionStatus.CLOSED);
        assertThat(statusOf(discarded)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(statusOf(otherItem)).isEqualTo(SuggestionStatus.NEW);
        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = 'MATCH_CLOSED'",
                String.class, fresh.getId())).singleElement().asString().contains("outra sugestão");
    }

    @Test
    void theStudentsOfTheClosedSuggestionsAreNotifiedOnceWithANeutralText() {
        UUID item = find();
        UUID winnerStudent = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        MatchSuggestion winner = suggest(item, lose(winnerStudent));
        MatchSuggestion first = suggest(item, lose(other));
        MatchSuggestion second = suggest(item, lose(other));
        Summons summons = summon(winner);

        counter.registerPickup(summons.getId(), check(), UUID.randomUUID());

        assertThat(events.stream(OtherSuggestionsClosedEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.foundItemId()).isEqualTo(item);
            assertThat(event.closed()).extracting(OtherSuggestionsClosedEvent.Closed::suggestionId)
                    .containsExactlyInAnyOrder(first.getId(), second.getId());
        });
        assertThat(closedNoticesTo(other)).hasSize(1).singleElement().satisfies(notification -> {
            assertThat(notification.getSubject()).isEqualTo("Cadê?: atualização sobre o seu registro de perda");
            assertThat(notification.getBody()).contains("continua aberto").doesNotContain("Fone")
                    .doesNotContain("Bloco");
        });
        assertThat(closedNoticesTo(winnerStudent)).isEmpty();
    }

    // ---- RF30: confirmação do estudante

    @Test
    void theStudentSeesThePickupAsUnconfirmedAndConfirmsIt() {
        UUID student = UUID.randomUUID();
        Pickup pickup = pickupOf(student);

        assertThat(confirmationService.mine(student)).singleElement().satisfies(mine -> {
            assertThat(mine.pickupId()).isEqualTo(pickup.getId());
            assertThat(mine.confirmed()).isFalse();
        });

        StudentPickup confirmed = confirmationService.confirm(pickup.getId(), student);

        assertThat(confirmed.confirmed()).isTrue();
        assertThat(confirmed.confirmedAt()).isEqualTo(clock.instant());
        assertThat(confirmationService.mine(student)).singleElement()
                .satisfies(mine -> assertThat(mine.confirmed()).isTrue());
        assertThat(jdbc.queryForList("SELECT action FROM audit_records WHERE entity_id = ? AND actor_id = ?",
                String.class, pickup.getId(), student)).containsExactly("PICKUP_CONFIRMED");
    }

    @Test
    void confirmingTwiceIsAConflictAndAnotherStudentSeesNotFound() {
        UUID student = UUID.randomUUID();
        Pickup pickup = pickupOf(student);
        confirmationService.confirm(pickup.getId(), student);

        assertThatThrownBy(() -> confirmationService.confirm(pickup.getId(), student))
                .isInstanceOf(PickupAlreadyConfirmedException.class);
        assertThatThrownBy(() -> confirmationService.confirm(pickup.getId(), UUID.randomUUID()))
                .isInstanceOf(PickupNotFoundException.class);
        assertThatThrownBy(() -> confirmationService.confirm(UUID.randomUUID(), student))
                .isInstanceOf(PickupNotFoundException.class);
        assertThat(confirmations.findAll()).filteredOn(c -> c.getPickupId().equals(pickup.getId())).hasSize(1);
    }

    @Test
    void theStudentOnlySeesHisOwnPickups() {
        UUID student = UUID.randomUUID();
        pickupOf(student);
        pickupOf(UUID.randomUUID());

        assertThat(confirmationService.mine(student)).hasSize(1);
        assertThat(confirmationService.mine(UUID.randomUUID())).isEmpty();
    }

    private Pickup pickupOf(UUID student) {
        MatchSuggestion suggestion = suggest(find(), lose(student));
        Summons summons = summon(suggestion);
        return counter.registerPickup(summons.getId(), check(), UUID.randomUUID());
    }

    private Summons summon(MatchSuggestion suggestion) {
        return summonsService.summon(suggestion.getId(), UUID.randomUUID()).summons();
    }

    private SuggestionStatus statusOf(MatchSuggestion suggestion) {
        return suggestions.findById(suggestion.getId()).orElseThrow().getStatus();
    }

    private List<Notification> closedNoticesTo(UUID student) {
        return notifications.findAll().stream()
                .filter(n -> n.getKind() == NotificationKind.SUGGESTION_CLOSED && n.getRecipientId().equals(student))
                .toList();
    }

    private static CounterCheck check() {
        return new CounterCheck(IdDocumentType.STUDENT_CARD, "000000", "000000000",
                VerificationMethod.DETAIL_CONFIRMED, null);
    }

    private MatchSuggestion suggest(UUID item, UUID report) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, 80, clock.instant()));
    }

    private UUID find() {
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400",
                "Sala 421", LocalDate.now(clock), null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    private UUID lose(UUID student) {
        return lossReportService.register(new RegisterLossReport(reportCategory, "Fone JBL preto", "Adesivo",
                List.of(LossLocation.of("Bloco 400", "Sala 421")), LocalDate.now(clock), null, null, null),
                student).getId();
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }
}
