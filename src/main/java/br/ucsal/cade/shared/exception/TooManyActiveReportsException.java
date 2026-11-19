package br.ucsal.cade.shared.exception;

/** O estudante já tem o máximo de registros em andamento e não pode abrir outro (RN05). */
public class TooManyActiveReportsException extends ConflictException {

    public TooManyActiveReportsException(int limit) {
        super("Você já tem " + limit + " registros em andamento. Cancele um que não precisa mais, ou espere um "
                + "deles ser resolvido, para registrar outra perda.");
    }
}
