package br.ucsal.cade.shared.exception;

/** Mudança de perfil que não faz sentido para a conta (RF03): responde 409. */
public class RoleChangeNotAllowedException extends ConflictException {

    private RoleChangeNotAllowedException(String userMessage) {
        super(userMessage);
    }

    public static RoleChangeNotAllowedException alreadyStaff() {
        return new RoleChangeNotAllowedException("Esta conta já tem o perfil de funcionário.");
    }

    public static RoleChangeNotAllowedException notStaff() {
        return new RoleChangeNotAllowedException("Esta conta não tem o perfil de funcionário.");
    }

    /** A conta do administrador é criada na instalação e não muda de perfil por aqui. */
    public static RoleChangeNotAllowedException adminAccount() {
        return new RoleChangeNotAllowedException("O perfil do administrador não pode ser alterado.");
    }
}
