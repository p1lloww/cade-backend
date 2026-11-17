package br.ucsal.cade.shared.exception;

/**
 * Uma ação foi tentada numa situação que não a permite (RN01). Serve para os três ciclos de vida da seção 6 dos
 * requisitos: objeto, registro de perda e sugestão. Responde 409, com a mensagem pronta para o usuário.
 */
public class InvalidStatusTransitionException extends ConflictException {

    /**
     * @param action      a ação no infinitivo, como o usuário a veria (por exemplo, "convocar")
     * @param subject     o que sofre a ação, com artigo (por exemplo, "um objeto")
     * @param statusLabel a situação atual, em português (por exemplo, "Devolvido")
     */
    public InvalidStatusTransitionException(String action, String subject, String statusLabel) {
        super("Não é possível " + action + " " + subject + " na situação " + statusLabel + ".");
    }
}
