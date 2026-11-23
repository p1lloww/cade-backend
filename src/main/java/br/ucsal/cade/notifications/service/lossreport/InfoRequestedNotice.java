package br.ucsal.cade.notifications.service.lossreport;

import br.ucsal.cade.notifications.service.outbox.enqueue.NotificationContent;

/**
 * O e-mail do pedido de mais informações (RF35). Leva o texto que o funcionário escreveu para o estudante; o funcionário
 * é orientado a não citar o objeto achado (RN03). Diz também o prazo, porque sem resposta o pedido se encerra (RN13).
 */
final class InfoRequestedNotice {

    static final String SUBJECT = "Cadê?: precisamos de mais informações sobre a sua perda";

    private InfoRequestedNotice() {
    }

    static NotificationContent from(String reason, int days) {
        String body = """
                Olá!

                O funcionário da sala de achados e perdidos precisa de mais informações para conferir o seu registro:

                "%s"

                Entre no Cadê? e responda em "Meus registros" em até %d dias. Sem resposta, o pedido é encerrado e o \
                seu registro continua aberto.

                Cadê? — Achados e Perdidos da UCSal
                """.formatted(reason, days);
        return new NotificationContent(SUBJECT, body);
    }
}
