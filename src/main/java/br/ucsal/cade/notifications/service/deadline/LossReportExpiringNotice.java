package br.ucsal.cade.notifications.service.deadline;

import br.ucsal.cade.lossreports.api.ExpiringLossReport;
import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;
import br.ucsal.cade.shared.time.TimeConfiguration;

import java.time.format.DateTimeFormatter;

/**
 * O lembrete de que o registro de perda vai vencer (RF35). Leva a descrição que o próprio estudante escreveu, para ele
 * reconhecer o registro; nada de objeto achado (RN03).
 */
final class LossReportExpiringNotice {

    static final String SUBJECT = "Cadê?: seu registro de perda vai vencer";

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(TimeConfiguration.ZONE);

    private LossReportExpiringNotice() {
    }

    static NotificationContent from(ExpiringLossReport report) {
        String body = """
                Olá!

                O seu registro de perda "%s" vence em %s.

                Se você ainda procura o objeto, entre no Cadê? e renove o registro em "Meus registros". Se já achou, pode ignorar este \
                e-mail.

                Cadê? — Achados e Perdidos da UCSal
                """.formatted(report.description(), DATE.format(report.expiresAt()));
        return new NotificationContent(SUBJECT, body);
    }
}
