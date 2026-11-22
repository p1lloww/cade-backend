package br.ucsal.cade.notifications.service.deadline;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;
import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.shared.time.TimeConfiguration;

import java.time.format.DateTimeFormatter;

/** O lembrete de que a convocação vai vencer (RF34), com o mesmo onde, horário e o que levar da convocação. */
final class SummonsExpiringNotice {

    static final String SUBJECT = "Cadê?: sua convocação vence amanhã";

    private static final DateTimeFormatter DEADLINE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(TimeConfiguration.ZONE);

    private SummonsExpiringNotice() {
    }

    static NotificationContent from(PickupInstructions instructions) {
        String body = """
                Olá!

                O objeto que pode ser seu continua reservado só até %s.

                Para retirar:
                - Onde: %s
                - Horário: %s
                - O que levar: %s

                Depois do prazo, a reserva acaba e o seu registro volta a ficar aberto.

                Cadê? — Achados e Perdidos da UCSal
                """.formatted(DEADLINE.format(instructions.deadline()), instructions.place(), instructions.hours(),
                instructions.whatToBring());
        return new NotificationContent(SUBJECT, body);
    }
}
