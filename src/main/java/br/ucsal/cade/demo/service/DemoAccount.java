package br.ucsal.cade.demo.service;

import br.ucsal.cade.shared.security.Role;

import java.util.Arrays;
import java.util.Optional;

/**
 * As contas fictícias do modo demonstração (RF47): uma por perfil. Os e-mails são inventados e não recebem nada: o
 * login direto dispensa o código.
 */
public enum DemoAccount {

    STUDENT(Role.STUDENT, "estudante.demo@ucsal.edu.br", "Estudante de demonstração"),
    STAFF(Role.STAFF, "funcionario.demo@ucsal.edu.br", "Funcionário de demonstração"),
    ADMIN(Role.ADMIN, "admin.demo@ucsal.edu.br", "Administrador de demonstração");

    private final Role role;
    private final String email;
    private final String label;

    DemoAccount(Role role, String email, String label) {
        this.role = role;
        this.email = email;
        this.label = label;
    }

    public Role role() {
        return role;
    }

    public String email() {
        return email;
    }

    public String label() {
        return label;
    }

    public static Optional<DemoAccount> of(Role role) {
        return Arrays.stream(values()).filter(account -> account.role == role).findFirst();
    }
}
