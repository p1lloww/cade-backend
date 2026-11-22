package br.ucsal.cade.notifications.persistence.outbox;

/** A situação de um aviso na caixa de saída. */
public enum NotificationStatus {

    /** Ainda não saiu: espera o envio logo depois do commit ou a próxima rodada do reenvio. */
    PENDING,

    /** O servidor de e-mail aceitou. */
    SENT,

    /** Esgotou as tentativas, ou o destinatário não tem mais conta: fica só para consulta. */
    ABANDONED
}
