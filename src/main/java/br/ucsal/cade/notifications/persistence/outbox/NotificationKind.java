package br.ucsal.cade.notifications.persistence.outbox;

/**
 * O assunto do aviso. Com a referência, identifica o aviso: o mesmo tipo para a mesma referência sai uma vez só.
 */
public enum NotificationKind {

    /** Estudante convocado; a referência é a convocação (RF34). */
    SUMMONS_CREATED,

    /** Falta menos de 1 dia para a convocação vencer; a referência é a convocação (RF34). */
    SUMMONS_EXPIRING,

    /** A convocação venceu sem retirada; a referência é a convocação (RF27). */
    SUMMONS_EXPIRED,

    /** Retirada registrada; a referência é a retirada (RF34). */
    PICKUP_REGISTERED,

    /** O funcionário pediu mais informações sobre o registro; a referência é o pedido (RF35). */
    INFO_REQUESTED,

    /** O registro de perda está perto de vencer; a referência é o registro (RF35). */
    LOSS_REPORT_EXPIRING
}
