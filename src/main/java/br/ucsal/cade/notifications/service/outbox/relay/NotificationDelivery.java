package br.ucsal.cade.notifications.service.outbox.relay;

import br.ucsal.cade.access.api.UserEmails;
import br.ucsal.cade.notifications.persistence.outbox.Notification;
import br.ucsal.cade.notifications.persistence.outbox.NotificationRepository;
import br.ucsal.cade.notifications.service.outbox.config.OutboxProperties;
import br.ucsal.cade.shared.mail.EmailMessage;
import br.ucsal.cade.shared.mail.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Uma tentativa de envio de um aviso, numa transação própria. A linha fica travada até o fim dela, então o envio logo
 * depois do commit e a rodada do reenvio nunca mandam o mesmo aviso duas vezes.
 */
@Service
class NotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelivery.class);

    private final NotificationRepository notifications;
    private final UserEmails userEmails;
    private final EmailSender emailSender;
    private final OutboxProperties properties;
    private final Clock clock;

    NotificationDelivery(NotificationRepository notifications, UserEmails userEmails, EmailSender emailSender,
                         OutboxProperties properties, Clock clock) {
        this.notifications = notifications;
        this.userEmails = userEmails;
        this.emailSender = emailSender;
        this.properties = properties;
        this.clock = clock;
    }

    /** Não faz nada se o aviso já saiu, foi abandonado ou está sendo enviado por outra thread. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(UUID notificationId) {
        Optional<Notification> locked = notifications.lockPending(notificationId);
        if (locked.isEmpty()) {
            log.debug("Notification {} is not pending or is being delivered elsewhere; skipped", notificationId);
            return;
        }
        Notification notification = locked.get();

        Optional<String> email = userEmails.emailOf(notification.getRecipientId());
        if (email.isEmpty()) {
            notification.abandon();
            log.warn("Notification {} abandoned: recipient {} has no account", notificationId,
                    notification.getRecipientId());
            return;
        }

        if (emailSender.send(new EmailMessage(email.get(), notification.getSubject(), notification.getBody()))) {
            notification.markSent(clock.instant());
            log.info("Notification {} sent: kind={}, reference={}, attempt={}", notificationId, notification.getKind(),
                    notification.getReferenceId(), notification.getAttempts());
        } else {
            notification.markFailed(clock.instant(), properties.maxAttempts(), properties.retryDelay());
            log.warn("Notification {} failed: kind={}, attempt={}/{}, status={}", notificationId,
                    notification.getKind(), notification.getAttempts(), properties.maxAttempts(),
                    notification.getStatus());
        }
    }
}
