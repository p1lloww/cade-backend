package br.ucsal.cade.lossreports.api;

import java.util.UUID;

/** A perda passa a Convocado na convocação (RF26), chamada pelo módulo de convocação e retirada. */
public interface LossReportSummons {

    /**
     * Aberto → Convocado, com o registro no histórico (RF43). Daí em diante a descrição e o detalhe secreto ficam
     * travados (RN06). Só roda dentro da transação da convocação (seção 7.1).
     *
     * @param staffId id do funcionário que convoca
     * @throws br.ucsal.cade.shared.exception.NotFoundException se o registro não existe
     * @throws br.ucsal.cade.shared.exception.InvalidStatusTransitionException se o registro não está Aberto
     */
    SummonedLossReport summon(UUID lossReportId, UUID staffId);
}
