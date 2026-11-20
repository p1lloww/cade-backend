package br.ucsal.cade.shared.exception;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException() {
        super("Conta não encontrada.");
    }
}
