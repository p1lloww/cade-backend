package br.ucsal.cade.lossreports.persistence.reporter;

import br.ucsal.cade.shared.exception.ConflictException;

/** Liberar um estudante que não está bloqueado (RN12): responde 409. */
public class StudentNotBlockedException extends ConflictException {

    public StudentNotBlockedException() {
        super("Este estudante não está bloqueado: ele tem menos de " + LossReporter.MAX_REFUSALS
                + " retiradas recusadas no balcão.");
    }
}
