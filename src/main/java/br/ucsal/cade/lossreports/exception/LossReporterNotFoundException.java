package br.ucsal.cade.lossreports.exception;

import br.ucsal.cade.shared.exception.NotFoundException;

public class LossReporterNotFoundException extends NotFoundException {

    public LossReporterNotFoundException() {
        super("Estudante não encontrado entre os que registram perdas.");
    }
}
