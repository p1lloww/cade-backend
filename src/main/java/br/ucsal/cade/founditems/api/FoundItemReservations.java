package br.ucsal.cade.founditems.api;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;

import java.util.UUID;

/** Reserva do objeto na convocação (RF26) e o fim dela no balcão, chamados pelo módulo de convocação e retirada. */
public interface FoundItemReservations {

    /**
     * Disponível ou Não reclamado → Reservado, com o registro no histórico (RF43). Só roda dentro da transação da
     * convocação, para que objeto, perda e sugestão mudem juntos (seção 7.1).
     *
     * <p>A mudança é gravada na hora, antes do resto da convocação: assim uma convocação simultânea do mesmo objeto
     * espera aqui e perde no {@code @Version} (RN18), em vez de travar nas sugestões.
     *
     * @param staffId id do funcionário que convoca
     * @throws NotFoundException se o objeto não existe
     * @throws InvalidStatusTransitionException se o objeto não pode ser reservado
     */
    ReservedFoundItem reserve(UUID foundItemId, UUID staffId);

    /**
     * Reservado → Devolvido: o dono retirou o objeto (RF29), com o registro no histórico (RF43). Só roda dentro da
     * transação da retirada.
     *
     * @throws NotFoundException se o objeto não existe
     * @throws InvalidStatusTransitionException se o objeto já foi entregue ou destinado
     */
    void handOver(UUID foundItemId, UUID staffId);

    /**
     * Reservado → Disponível, com o registro no histórico (RF43): a verificação falhou no balcão ou a convocação
     * venceu. Só roda dentro da transação de quem libera.
     *
     * @param actorId id do funcionário, ou {@code AuditEvent.SYSTEM_ACTOR} numa tarefa agendada
     * @param reason  por que o objeto voltou, para o histórico
     * @throws NotFoundException se o objeto não existe
     * @throws InvalidStatusTransitionException se o objeto não está Reservado
     */
    void release(UUID foundItemId, UUID actorId, String reason);
}
