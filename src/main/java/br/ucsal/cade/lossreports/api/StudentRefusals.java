package br.ucsal.cade.lossreports.api;

import java.util.UUID;

/** As recusas no balcão que contam para o estudante (RN12), chamadas pelo módulo de convocação e retirada. */
public interface StudentRefusals {

    /**
     * Soma uma recusa ao estudante, com o registro no histórico (RF43). Na terceira, ele não abre novos registros de
     * perda até o administrador liberar. Só roda dentro da transação da recusa.
     *
     * @param staffId id do funcionário que recusou a entrega
     */
    RecordedRefusal record(UUID studentId, UUID staffId);
}
