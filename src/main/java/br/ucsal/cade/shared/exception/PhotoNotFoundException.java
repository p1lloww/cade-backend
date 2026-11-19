package br.ucsal.cade.shared.exception;

/** O registro existe, mas não tem foto, ou o arquivo dela não está mais no armazenamento. */
public class PhotoNotFoundException extends NotFoundException {

    public PhotoNotFoundException() {
        super("Foto não encontrada.");
    }
}
