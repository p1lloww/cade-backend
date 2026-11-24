package br.ucsal.cade.founditems.api.unclaimed;

import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Os objetos que passam do prazo de guarda e o destino deles (RF37, RF38, RF39), chamado pelo módulo de prazos. */
public interface UnclaimedFoundItems {

    /** Os ids dos objetos Disponíveis que entraram na sala até {@code registeredUntil}, os mais antigos primeiro. */
    List<UUID> idsDueForUnclaimed(Instant registeredUntil);

    /**
     * Disponível → Não reclamado, com o registro no histórico em nome do sistema (RF43). Uma transação por objeto: um
     * que falhe não desfaz os outros.
     *
     * @return {@code false} se o objeto já não está Disponível (foi reservado ou entregue enquanto a tarefa rodava)
     */
    boolean markUnclaimed(UUID foundItemId);

    /** Os não reclamados, por caixa (a do mês) e pela data em que foram achados (RF39). */
    List<UnclaimedFoundItem> listUnclaimed();

    /**
     * Não reclamado → Destinado, com o motivo no histórico. Só roda dentro da transação do destino.
     *
     * @throws NotFoundException                se o objeto não existe
     * @throws InvalidStatusTransitionException se o objeto não está Não reclamado
     */
    UnclaimedFoundItem dispose(UUID foundItemId, UUID adminId, String reason);
}
