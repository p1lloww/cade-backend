package br.ucsal.cade.shared.exception;

/** O registro de perda não existe ou é de outro estudante; os dois casos respondem igual, para não revelar nada. */
public class LossReportNotFoundException extends NotFoundException {

    public LossReportNotFoundException() {
        super("Registro de perda não encontrado.");
    }
}
