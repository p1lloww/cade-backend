package br.ucsal.cade.lossreports.api;

import java.util.UUID;

/**
 * Registro de perda por código, para o modo demonstração (RF47). Faz o mesmo que a rota do estudante: respeita o
 * limite de registros abertos, grava o histórico e publica o evento que dispara o cruzamento.
 */
public interface LossReportRegistration {

    /**
     * @param studentId id da conta do estudante dono do registro
     * @return o id do registro
     */
    UUID register(LossReportData data, UUID studentId);

    /** Se o estudante já tem algum registro de perda, em qualquer situação. */
    boolean hasAnyOf(UUID studentId);
}
