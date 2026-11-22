package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.service.counter.CounterService;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.ConflictException;
import br.ucsal.cade.shared.mail.EmailMessage;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Da convocação e da retirada de verdade até o e-mail. Objeto e perda ficam em categorias só do teste, para o
 * cruzamento automático não criar sugestões a mais.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PickupNoticeListenerTest {

    @Autowired
    private SummonsService summonsService;

    @Autowired
    private CounterService counterService;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private UserRepository users;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private EmailSender emailSender;

    private User student;

    @BeforeEach
    void setUp() {
        when(emailSender.send(any())).thenReturn(true);
        student = users.saveAndFlush(User.newStudent("convocado-" + UUID.randomUUID() + "@ucsal.edu.br",
                clock.instant()));
    }

    @Test
    void theSummonedStudentGetsWhereUntilWhenAndWhatToBringWithoutTheItemDetails() {
        Summons summons = summonsService.summon(suggestion().getId(), UUID.randomUUID()).summons();

        assertThat(notificationsOf(summons.getId())).singleElement().satisfies(notification -> {
            assertThat(notification.getKind()).isEqualTo(NotificationKind.SUMMONS_CREATED);
            assertThat(notification.getRecipientId()).isEqualTo(student.getId());
        });
        EmailMessage sent = sentEmail();
        assertThat(sent.to()).isEqualTo(student.getEmail());
        assertThat(sent.subject()).isEqualTo(SummonsCreatedNotice.SUBJECT);
        assertThat(sent.body())
                .contains("Sala de achados e perdidos da UCSal", "Das 7h às 21h50", "Um documento com foto")
                .doesNotContain("Fone preto", "detalhe reservado", "Bloco 400", "Sala 421");
    }

    @Test
    void theStudentGetsTheConfirmationOfThePickup() {
        Summons summons = summonsService.summon(suggestion().getId(), UUID.randomUUID()).summons();
        sentEmail();

        Pickup pickup = counterService.registerPickup(summons.getId(), new CounterCheck(IdDocumentType.STUDENT_CARD,
                "000000", "000000000", VerificationMethod.DETAIL_CONFIRMED, null), UUID.randomUUID());

        assertThat(notificationsOf(pickup.getId())).singleElement()
                .extracting(Notification::getKind).isEqualTo(NotificationKind.PICKUP_REGISTERED);
        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(5_000).times(2)).send(sent.capture());
        assertThat(sent.getAllValues().getLast().subject()).isEqualTo(PickupRegisteredNotice.SUBJECT);
        assertThat(sent.getAllValues().getLast().to()).isEqualTo(student.getEmail());
    }

    @Test
    void aSummonsThatIsRefusedQueuesNoEmail() {
        MatchSuggestion suggestion = suggestion();
        summonsService.summon(suggestion.getId(), UUID.randomUUID());
        sentEmail();

        assertThatThrownBy(() -> summonsService.summon(suggestion.getId(), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class);

        assertThat(notifications.findAll()).filteredOn(n -> n.getRecipientId().equals(student.getId()))
                .hasSize(1);
    }

    private EmailMessage sentEmail() {
        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(5_000)).send(sent.capture());
        return sent.getValue();
    }

    private List<Notification> notificationsOf(UUID reference) {
        return notifications.findAll().stream().filter(n -> n.getReferenceId().equals(reference)).toList();
    }

    private MatchSuggestion suggestion() {
        UUID item = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), newCategory(), "Bloco 400",
                "Sala 421", LocalDate.now(clock), null, RiskLevel.MEDIUM, "Fone preto", "detalhe reservado"),
                UUID.randomUUID()).getId();
        UUID report = lossReports.register(new RegisterLossReport(newCategory(), "Fone JBL preto", null, List.of(),
                LocalDate.now(clock), null, null, null), student.getId()).getId();
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, 80, clock.instant()));
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }
}
