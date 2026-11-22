package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;

/**
 * O e-mail de convocação vencida (RF27): o prazo passou sem retirada, o objeto deixou de estar reservado e o registro
 * de perda continua aberto. O texto não cita o objeto, porque o estudante só o conhece pela convocação (RN03).
 */
final class SummonsExpiredNotice {

    static final String SUBJECT = "Cadê?: a sua convocação venceu";

    private SummonsExpiredNotice() {
    }

    static NotificationContent create() {
        String body = """
                Olá!

                O prazo para retirar o objeto da sua convocação terminou, e ele deixou de ficar reservado para você.

                O seu registro de perda continua aberto: se a sala encontrar outro objeto parecido, você será \
                convocado de novo. Se ainda quiser aquele objeto, procure a sala de achados e perdidos o quanto antes.

                Cadê? — Achados e Perdidos da UCSal
                """;
        return new NotificationContent(SUBJECT, body);
    }
}
