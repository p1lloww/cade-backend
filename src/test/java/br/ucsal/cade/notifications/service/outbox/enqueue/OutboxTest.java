package br.ucsal.cade.notifications.service.outbox.enqueue;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.notifications.persistence.outbox.NotificationStatus;
import br.ucsal.cade.notifications.service.outbox.relay.OutboxRelay;
import br.ucsal.cade.shared.mail.EmailMessage;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A entrada da caixa de saída: o aviso vai junto com a ação (gravado ou desfeito com ela) e sai logo depois do commit.
 * O reenvio agendado não roda sozinho nos testes (relay-interval de 1 h): eles chamam o relay quando precisam.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class OutboxTest {

    private static final NotificationContent CONTENT = new NotificationContent("Assunto do aviso", "Texto do aviso");

    @Autowired
    private Outbox outbox;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private UserRepository users;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private MutableClock clock;

    @MockitoBean
    private EmailSender emailSender;

    private User student;

    @BeforeEach
    void setUp() {
        student = users.saveAndFlush(User.newStudent("outbox-" + UUID.randomUUID() + "@ucsal.edu.br",
                clock.instant()));
    }

    @Test
    void theNotificationIsOnlyQueuedInsideTheTransactionOfTheAction() {
        assertThatThrownBy(() -> outbox.enqueue(NotificationKind.SUMMONS_CREATED, UUID.randomUUID(), student.getId(),
                CONTENT)).isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void anActionThatIsRolledBackLeavesNoNotificationAndSendsNothing() {
        UUID reference = UUID.randomUUID();

        transaction.executeWithoutResult(status -> {
            outbox.enqueue(NotificationKind.SUMMONS_CREATED, reference, student.getId(), CONTENT);
            status.setRollbackOnly();
        });

        assertThat(notificationOf(reference)).isNull();
        verify(emailSender, never()).send(any());
    }

    @Test
    void aCommittedActionIsDeliveredRightAfterTheCommit() {
        when(emailSender.send(any())).thenReturn(true);
        UUID reference = UUID.randomUUID();

        transaction.executeWithoutResult(_ ->
                outbox.enqueue(NotificationKind.SUMMONS_CREATED, reference, student.getId(), CONTENT));

        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailSender, timeout(5_000)).send(sent.capture());
        assertThat(sent.getValue()).isEqualTo(new EmailMessage(student.getEmail(), "Assunto do aviso",
                "Texto do aviso"));
        Notification sentNotification = await(reference, n -> n.getStatus() == NotificationStatus.SENT);
        assertThat(sentNotification.getAttempts()).isOne();
        assertThat(sentNotification.getSentAt()).isEqualTo(clock.instant());
    }

    @Test
    void theSameNotificationIsQueuedOnlyOnce() {
        when(emailSender.send(any())).thenReturn(true);
        UUID reference = UUID.randomUUID();

        Boolean first = transaction.execute(_ ->
                outbox.enqueue(NotificationKind.SUMMONS_EXPIRING, reference, student.getId(), CONTENT));
        Boolean second = transaction.execute(_ ->
                outbox.enqueue(NotificationKind.SUMMONS_EXPIRING, reference, student.getId(), CONTENT));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        verify(emailSender, timeout(5_000).times(1)).send(any());
        assertThat(notifications.findAll()).filteredOn(n -> n.getReferenceId().equals(reference)).hasSize(1);
    }

    @Test
    void aFailedDeliveryStaysPendingAndTheRelayTriesAgainWhenItIsTime() {
        when(emailSender.send(any())).thenReturn(false, true);
        UUID reference = UUID.randomUUID();
        Instant start = clock.instant();

        transaction.executeWithoutResult(_ ->
                outbox.enqueue(NotificationKind.PICKUP_REGISTERED, reference, student.getId(), CONTENT));

        Notification failed = await(reference, n -> n.getAttempts() == 1);
        assertThat(failed.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(failed.getNextAttemptAt()).isEqualTo(start.plus(Duration.ofMinutes(5)));

        relay.relayPending();
        verify(emailSender, times(1)).send(any());

        clock.advance(Duration.ofMinutes(5));
        relay.relayPending();

        Notification sent = notificationOf(reference);
        assertThat(sent.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(sent.getAttempts()).isEqualTo(2);
        verify(emailSender, times(2)).send(any());
    }

    private Notification notificationOf(UUID reference) {
        return notifications.findAll().stream().filter(n -> n.getReferenceId().equals(reference)).findFirst()
                .orElse(null);
    }

    /** O envio logo após o commit roda em outra thread: espera até a linha chegar ao estado esperado. */
    private Notification await(UUID reference, Predicate<Notification> condition) {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            Notification notification = notificationOf(reference);
            if (notification != null && condition.test(notification)) {
                return notification;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        throw new AssertionError("A notificação não chegou ao estado esperado em 5 s: " + notificationOf(reference));
    }
}
