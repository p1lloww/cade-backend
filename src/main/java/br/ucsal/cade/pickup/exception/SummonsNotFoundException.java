package br.ucsal.cade.pickup.exception;

import br.ucsal.cade.shared.exception.NotFoundException;

public class SummonsNotFoundException extends NotFoundException {

    public SummonsNotFoundException() {
        super("Convocação não encontrada.");
    }
}
