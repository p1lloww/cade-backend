package br.ucsal.cade.lossreports.exception;

import br.ucsal.cade.lossreports.persistence.reporter.LossReporter;
import br.ucsal.cade.shared.exception.ConflictException;

public class StudentBlockedException extends ConflictException {

    public StudentBlockedException() {
        super("Você teve " + LossReporter.MAX_REFUSALS + " retiradas recusadas no balcão e não pode registrar novas "
                + "perdas por enquanto. Procure a sala de achados e perdidos para liberar o seu acesso.");
    }
}
