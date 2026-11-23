package br.ucsal.cade.pickup.exception;

import br.ucsal.cade.shared.exception.ConflictException;

public class PickupAlreadyConfirmedException extends ConflictException {

    public PickupAlreadyConfirmedException() {
        super("Você já confirmou esta retirada.");
    }
}
