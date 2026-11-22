package br.ucsal.cade.notifications.service.outbox.relay;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationKind;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.notifications.persistence.outbox.NotificationStatus;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A saída da caixa de saída: cada tentativa de envio, a espera que cresce, o abandono e a trava que impede o mesmo
 * aviso de sair duas vezes. Os avisos são gravados direto no repositório, sem o envio logo após o commit, para o
 * teste controlar cada tentativa.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class NotificationDeliveryTest {

    @Autowired
    private NotificationDelivery delivery;

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
    void theWaitGrowsAtEachFailureAndTheNotificationIsAbandonedAfterTheLastAttempt() {
        when(emailSender.send(any())).thenReturn(false);
        UUID reference = UUID.randomUUID();
        UUID id = queueWithoutImmediateDelivery(reference);

        for (int attempt = 1; attempt <= 5; attempt++) {
            delivery.deliver(id);
            Notification notification = notificationOf(reference);
            assertThat(notification.getAttempts()).isEqualTo(attempt);
            if (attempt < 5) {
                assertThat(notification.getNextAttemptAt())
                        .isEqualTo(clock.instant().plus(Duration.ofMinutes(5L * attempt)));
            }
        }

        assertThat(notificationOf(reference).getStatus()).isEqualTo(NotificationStatus.ABANDONED);
        delivery.deliver(id);
        verify(emailSender, times(5)).send(any());
    }

    @Test
    void aRecipientWithoutAccountIsAbandonedWithoutSending() {
        UUID reference = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        transaction.executeWithoutResult(_ -> notifications.insertIfAbsent(id, NotificationKind.SUMMONS_CREATED.name(),
                reference, UUID.randomUUID(), "Assunto", "Texto", clock.instant()));

        delivery.deliver(id);

        assertThat(notificationOf(reference).getStatus()).isEqualTo(NotificationStatus.ABANDONED);
        verify(emailSender, never()).send(any());
    }

    @Test
    void twoDeliveriesOfTheSameNotificationAtTheSameTimeSendItOnce() throws Exception {
        when(emailSender.send(any())).thenAnswer(_ -> {
            Thread.sleep(300);
            return true;
        });
        UUID reference = UUID.randomUUID();
        UUID id = queueWithoutImmediateDelivery(reference);

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> runs = List.of(
                    executor.submit(() -> {
                        start.await();
                        delivery.deliver(id);
                        return null;
                    }),
                    executor.submit(() -> {
                        start.await();
                        delivery.deliver(id);
                        return null;
                    }));
            start.countDown();
            for (Future<?> run : runs) {
                run.get();
            }
        }

        verify(emailSender, times(1)).send(any());
        assertThat(notificationOf(reference).getStatus()).isEqualTo(NotificationStatus.SENT);
    }

    /** Grava direto no repositório, sem o envio logo após o commit, para o teste controlar cada tentativa. */
    private UUID queueWithoutImmediateDelivery(UUID reference) {
        UUID id = UUID.randomUUID();
        transaction.executeWithoutResult(_ -> notifications.insertIfAbsent(id,
                NotificationKind.LOSS_REPORT_EXPIRING.name(), reference, student.getId(), "Assunto", "Texto",
                clock.instant()));
        return id;
    }

    private Notification notificationOf(UUID reference) {
        return notifications.findAll().stream().filter(n -> n.getReferenceId().equals(reference)).findFirst()
                .orElse(null);
    }
}
