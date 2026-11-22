package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;
import br.ucsal.cade.pickup.api.PickupInstructions;
import br.ucsal.cade.shared.time.TimeConfiguration;

import java.time.format.DateTimeFormatter;

/**
 * O e-mail da convocação (RF34): onde, em que horário, até quando e o que levar. Não fala do objeto achado (RN03):
 * quem confere se ele é do estudante é o funcionário, no balcão.
 */
final class SummonsCreatedNotice {

    static final String SUBJECT = "Cadê?: encontramos um objeto que pode ser seu";

    private static final DateTimeFormatter DEADLINE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(TimeConfiguration.ZONE);

    private SummonsCreatedNotice() {
    }

    static NotificationContent from(PickupInstructions instructions) {
        String body = """
                Olá!

                A sala de achados e perdidos encontrou um objeto que pode ser o que você registrou no Cadê?.

                Para retirar:
                - Onde: %s
                - Horário: %s
                - Até: %s
                - O que levar: %s

                Só você pode retirar o objeto. No balcão, o funcionário confere se ele é mesmo seu.
                Se você não for até o prazo, a reserva acaba e o seu registro volta a ficar aberto.

                Cadê? — Achados e Perdidos da UCSal
                """.formatted(instructions.place(), instructions.hours(), DEADLINE.format(instructions.deadline()),
                instructions.whatToBring());
        return new NotificationContent(SUBJECT, body);
    }
}
