package br.ucsal.cade.pickup.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Uma convocação Ativa perto de vencer, com o que o estudante precisa para ainda retirar o objeto (RF34). Não leva
 * nada reservado do objeto (RN03).
 */
public record ExpiringSummons(UUID summonsId, UUID studentId, PickupInstructions instructions) {

    public ExpiringSummons {
        Objects.requireNonNull(summonsId);
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(instructions);
    }
}
