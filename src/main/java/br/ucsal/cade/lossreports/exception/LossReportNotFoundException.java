package br.ucsal.cade.lossreports.exception;

import br.ucsal.cade.shared.exception.NotFoundException;

/** Inexistente ou de outro estudante: os dois casos respondem igual (RF14). */
public class LossReportNotFoundException extends NotFoundException {

    public LossReportNotFoundException() {
        super("Registro de perda não encontrado.");
    }
}
