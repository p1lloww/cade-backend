package br.ucsal.cade.founditems.api;

import java.util.UUID;

/** Reserva do objeto na convocação (RF26), chamada pelo módulo de convocação e retirada. */
public interface FoundItemReservations {

    /**
     * Disponível ou Não reclamado → Reservado, com o registro no histórico (RF43). Só roda dentro da transação da
     * convocação, para que objeto, perda e sugestão mudem juntos (seção 7.1).
     *
     * <p>A mudança é gravada na hora, antes do resto da convocação: assim uma convocação simultânea do mesmo objeto
     * espera aqui e perde no {@code @Version} (RN18), em vez de travar nas sugestões.
     *
     * @param staffId id do funcionário que convoca
     * @throws br.ucsal.cade.shared.exception.NotFoundException se o objeto não existe
     * @throws br.ucsal.cade.shared.exception.InvalidStatusTransitionException se o objeto não pode ser reservado
     */
    ReservedFoundItem reserve(UUID foundItemId, UUID staffId);
}
