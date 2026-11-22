package br.ucsal.cade.notifications.service.outbox.enqueue;

import br.ucsal.cade.notifications.persistence.outbox.Notification;

import java.util.Objects;

/** O assunto e o texto de um aviso, já prontos. Nunca levam segredo: ficam gravados na caixa de saída. */
public record NotificationContent(String subject, String body) {

    public NotificationContent {
        Objects.requireNonNull(subject);
        Objects.requireNonNull(body);
        if (subject.length() > Notification.MAX_SUBJECT_LENGTH || body.length() > Notification.MAX_BODY_LENGTH) {
            throw new IllegalArgumentException("Aviso maior que o limite da caixa de saída.");
        }
    }
}
