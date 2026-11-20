package br.ucsal.cade.shared.exception;

public class PhotoNotFoundException extends NotFoundException {

    public PhotoNotFoundException() {
        super("Foto não encontrada.");
    }
}
