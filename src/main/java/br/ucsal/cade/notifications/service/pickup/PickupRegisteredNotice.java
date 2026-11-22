package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;
import br.ucsal.cade.shared.time.TimeConfiguration;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * O e-mail de confirmação da retirada (RF34). Também serve de alerta: se o estudante não retirou nada, ele fica
 * sabendo e procura a sala.
 */
final class PickupRegisteredNotice {

    static final String SUBJECT = "Cadê?: retirada registrada";

    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").withZone(TimeConfiguration.ZONE);

    private PickupRegisteredNotice() {
    }

    static NotificationContent at(Instant registeredAt) {
        String body = """
                Olá!

                A retirada do seu objeto foi registrada no balcão em %s, e o seu registro de perda foi concluído.

                Se não foi você quem retirou, procure a sala de achados e perdidos.

                Cadê? — Achados e Perdidos da UCSal
                """.formatted(WHEN.format(registeredAt));
        return new NotificationContent(SUBJECT, body);
    }
}
