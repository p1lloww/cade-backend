package br.ucsal.cade.notifications.service.pickup;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;

/**
 * O aviso de que uma análise de objeto para o registro do estudante foi encerrada porque o objeto foi entregue (RF33).
 * O texto é de propósito vago: não diz qual objeto, nem como ele é, nem para quem foi entregue, porque o estudante só
 * pode conhecer um objeto a partir da convocação (RN03, D29). Diz o que importa: o registro continua aberto.
 */
final class SuggestionClosedNotice {

    static final String SUBJECT = "Cadê?: atualização sobre o seu registro de perda";

    private SuggestionClosedNotice() {
    }

    static NotificationContent create() {
        String body = """
                Olá!

                Uma análise que a sala de achados e perdidos fazia para o seu registro foi encerrada, porque o objeto \
                analisado foi entregue.

                Isso não muda nada no seu registro: ele continua aberto, e você será avisado se aparecer outro \
                objeto que combine.

                Cadê? — Achados e Perdidos da UCSal
                """;
        return new NotificationContent(SUBJECT, body);
    }
}
