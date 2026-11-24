package br.ucsal.cade.access.exception;

import br.ucsal.cade.shared.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("Conta não encontrada.");
    }
}
