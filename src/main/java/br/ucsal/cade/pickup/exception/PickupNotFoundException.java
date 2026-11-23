package br.ucsal.cade.pickup.exception;

import br.ucsal.cade.shared.exception.NotFoundException;

/** Retirada de outro estudante responde como inexistente (RF14). */
public class PickupNotFoundException extends NotFoundException {

    public PickupNotFoundException() {
        super("Retirada não encontrada.");
    }
}
