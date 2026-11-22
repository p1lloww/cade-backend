package br.ucsal.cade.pickup.service.walkin;

import br.ucsal.cade.shared.exception.BadRequestException;

public class EnrollmentRequiredException extends BadRequestException {

    public EnrollmentRequiredException() {
        super("Informe a matrícula: para estudantes, ela também é registrada na retirada.");
    }
}
