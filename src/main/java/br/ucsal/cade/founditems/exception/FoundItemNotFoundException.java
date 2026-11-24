package br.ucsal.cade.founditems.exception;

import br.ucsal.cade.shared.exception.NotFoundException;

public class FoundItemNotFoundException extends NotFoundException {

    public FoundItemNotFoundException() {
        super("Objeto não encontrado.");
    }
}
