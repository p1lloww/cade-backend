package br.ucsal.cade.pickup.service.expiry;

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
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.pickup.service.summons.IssuedSummons;
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

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * O vencimento da convocação (RF27, RN15) de ponta a ponta: a tarefa roda sozinha só de hora em hora, então aqui ela é
 * chamada direto, com o relógio controlado. Objetos e perdas ficam em categorias criadas só para cada teste, para o
 * cruzamento automático não gravar sugestões além das que o teste cria.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class SummonsExpiryJobTest {

    private static final Duration DEADLINE = Duration.ofDays(7);

    @Autowired
    private SummonsExpiryJob job;

    @Autowired
    private SummonsExpiryService expiry;

    @Autowired
    private SummonsService summonsService;

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
    private NotificationRepository notifications;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

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

    @Test
    void expiresTheSummonsAfterTheDeadlineAndFreesTheItemAndTheReport() {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        Summons summons = summon(suggestion);

        clock.advance(DEADLINE.plusMinutes(1));
        job.expireDueSummonses();

        Summons expired = summonses.findById(summons.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(SummonsStatus.EXPIRED);
        assertThat(expired.getClosedAt()).isEqualTo(clock.instant());
        assertThat(expired.getClosedBy()).isNull();
        assertThat(foundItems.findById(suggestion.getFoundItemId()).orElseThrow().getStatus())
                .isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(lossReports.findById(suggestion.getLossReportId()).orElseThrow().getStatus())
                .isEqualTo(LossReportStatus.OPEN);
        assertThat(statusOf(suggestion)).isEqualTo(SuggestionStatus.EXPIRED);
    }

    @Test
    void theSuggestionsOnHoldOfTheSameItemGoBackToNew() {
        UUID item = find();
        MatchSuggestion summoned = suggest(item, lose(), 80);
        MatchSuggestion onHold = suggest(item, lose(), 50);
        MatchSuggestion discarded = suggest(item, lose(), 45);
        discarded.discard();
        suggestions.saveAndFlush(discarded);
        summon(summoned);
        assertThat(statusOf(onHold)).isEqualTo(SuggestionStatus.ON_HOLD);

        clock.advance(DEADLINE.plusMinutes(1));
        job.expireDueSummonses();

        assertThat(statusOf(onHold)).isEqualTo(SuggestionStatus.NEW);
        assertThat(statusOf(discarded)).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(lossReports.findById(onHold.getLossReportId()).orElseThrow().getStatus())
                .isEqualTo(LossReportStatus.OPEN);
    }

    @Test
    void doesNotExpireBeforeTheDeadline() {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        Summons summons = summon(suggestion);

        clock.advance(DEADLINE.minusMinutes(1));
        job.expireDueSummonses();

        assertThat(summonses.findById(summons.getId()).orElseThrow().getStatus()).isEqualTo(SummonsStatus.ACTIVE);
        assertThat(foundItems.findById(suggestion.getFoundItemId()).orElseThrow().getStatus())
                .isEqualTo(FoundItemStatus.RESERVED);
        assertThat(statusOf(suggestion)).isEqualTo(SuggestionStatus.SUMMONED);
    }

    @Test
    void expiresExactlyAtTheDeadline() {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        Summons summons = summon(suggestion);

        clock.advance(DEADLINE);
        job.expireDueSummonses();

        assertThat(summonses.findById(summons.getId()).orElseThrow().getStatus()).isEqualTo(SummonsStatus.EXPIRED);
    }

    @Test
    void aSummonsAlreadyClosedAtTheCounterIsLeftAlone() {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        Summons summons = summon(suggestion);
        UUID due = summons.getId();
        clock.advance(DEADLINE.plusMinutes(1));
        assertThat(expiry.dueIds()).contains(due);

        // O balcão registra a retirada depois de a tarefa ter listado a convocação.
        Summons reloaded = summonses.findById(due).orElseThrow();
        reloaded.complete(UUID.randomUUID(), clock.instant());
        summonses.saveAndFlush(reloaded);

        assertThat(expiry.expire(due)).isFalse();
        assertThat(summonses.findById(due).orElseThrow().getStatus()).isEqualTo(SummonsStatus.COMPLETED);
    }

    @Test
    void queuesOneEmailToTheStudentWithoutNamingTheObject() {
        UUID student = UUID.randomUUID();
        MatchSuggestion suggestion = suggest(find(), lose(student), 80);
        Summons summons = summon(suggestion);
        clock.advance(DEADLINE.plusMinutes(1));

        job.expireDueSummonses();

        assertThat(notificationsOf(summons.getId())).singleElement().satisfies(notification -> {
            assertThat(notification.getKind()).isEqualTo(NotificationKind.SUMMONS_EXPIRED);
            assertThat(notification.getRecipientId()).isEqualTo(student);
            assertThat(notification.getSubject()).isEqualTo("Cadê?: a sua convocação venceu");
            assertThat(notification.getBody()).contains("continua aberto").doesNotContain("Fone");
        });
    }

    private Summons summon(MatchSuggestion suggestion) {
        IssuedSummons issued = summonsService.summon(suggestion.getId(), UUID.randomUUID());
        return issued.summons();
    }

    private SuggestionStatus statusOf(MatchSuggestion suggestion) {
        return suggestions.findById(suggestion.getId()).orElseThrow().getStatus();
    }

    /** Só os avisos de vencimento: a convocação também gerou o aviso de convocada. */
    private List<Notification> notificationsOf(UUID summonsId) {
        return notifications.findAll().stream()
                .filter(n -> n.getReferenceId().equals(summonsId) && n.getKind() == NotificationKind.SUMMONS_EXPIRED)
                .toList();
    }

    private MatchSuggestion suggest(UUID item, UUID report, int score) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, score, clock.instant()));
    }

    private UUID find() {
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400",
                "Sala 421", LocalDate.now(clock), null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    private UUID lose() {
        return lose(UUID.randomUUID());
    }

    private UUID lose(UUID owner) {
        return lossReportService.register(new RegisterLossReport(reportCategory, "Fone JBL preto",
                "Adesivo de estrela", List.of(LossLocation.of("Bloco 400", "Sala 421")), LocalDate.now(clock), null,
                null, null), owner).getId();
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }
}
