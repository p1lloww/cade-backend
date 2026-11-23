package br.ucsal.cade.lossreports.api.event;

import java.util.UUID;

/**
 * O registro de perda se encerrou sem retirada, por cancelamento do estudante ou por vencimento (PER-06). Publicado
 * na transação que encerrou: as sugestões abertas dele deixam de fazer sentido e são encerradas na mesma transação.
 */
public record LossReportClosedEvent(UUID lossReportId, Reason reason) {

    public enum Reason {
        /** O estudante cancelou ("já achei", RF17). */
        CANCELLED,
        /** Passaram 30 dias sem renovação (RN14). */
        EXPIRED
    }
}
