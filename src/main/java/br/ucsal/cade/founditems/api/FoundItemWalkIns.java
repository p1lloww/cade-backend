package br.ucsal.cade.founditems.api;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;

import java.util.UUID;

/** O objeto no atalho de balcão, a retirada sem registro de perda (RF32), chamado pelo módulo de retirada. */
public interface FoundItemWalkIns {

    /**
     * O risco e a situação do objeto, para o funcionário ver o que conferir antes de entregar (RF28).
     *
     * @throws NotFoundException se o objeto não existe
     */
    WalkInFoundItem find(UUID foundItemId);

    /**
     * Disponível ou Não reclamado → Devolvido, com o registro no histórico (RF43). Só roda dentro da transação da
     * retirada.
     *
     * <p>A mudança é gravada na hora, com o {@code @Version}: um atalho e uma convocação simultâneos do mesmo objeto
     * disputam a mesma linha, e o segundo perde (409) sem mudar nada.
     *
     * @throws NotFoundException se o objeto não existe
     * @throws InvalidStatusTransitionException se o objeto está Reservado para um convocado (RN18), Devolvido ou
     *                                          Destinado
     */
    WalkInFoundItem handOverWithoutSummons(UUID foundItemId, UUID staffId);
}
