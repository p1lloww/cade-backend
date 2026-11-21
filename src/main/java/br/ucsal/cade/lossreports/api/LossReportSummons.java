package br.ucsal.cade.lossreports.api;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;

import java.util.UUID;

/** A perda na convocação (RF26) e no fim dela no balcão, chamada pelo módulo de convocação e retirada. */
public interface LossReportSummons {

    /**
     * Aberto → Convocado, com o registro no histórico (RF43). Daí em diante a descrição e o detalhe secreto ficam
     * travados (RN06). Só roda dentro da transação da convocação (seção 7.1).
     *
     * @param staffId id do funcionário que convoca
     * @throws NotFoundException se o registro não existe
     * @throws InvalidStatusTransitionException se o registro não está Aberto
     */
    SummonedLossReport summon(UUID lossReportId, UUID staffId);

    /**
     * Convocado → Concluído: o estudante retirou o objeto (RF29), com o registro no histórico (RF43). Só roda dentro da
     * transação da retirada.
     *
     * @throws NotFoundException se o registro não existe
     * @throws InvalidStatusTransitionException se o registro não está Convocado
     */
    void complete(UUID lossReportId, UUID staffId);

    /**
     * Convocado → Aberto, com o registro no histórico (RF43): a verificação falhou no balcão ou a convocação venceu.
     * Só roda dentro da transação de quem libera.
     *
     * @param actorId id do funcionário, ou {@code AuditEvent.SYSTEM_ACTOR} numa tarefa agendada
     * @param reason  por que o registro voltou, para o histórico
     * @throws NotFoundException se o registro não existe
     * @throws InvalidStatusTransitionException se o registro não está Convocado
     */
    void release(UUID lossReportId, UUID actorId, String reason);
}
