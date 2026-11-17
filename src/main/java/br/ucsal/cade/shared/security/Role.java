package br.ucsal.cade.shared.security;

/**
 * Perfil de uma conta (RF02). Cada conta tem exatamente um perfil; visitantes não têm conta.
 *
 * <p>Fica no núcleo compartilhado porque a autorização por perfil ({@code @RequiresRole}) é usada pelos controllers de
 * todos os módulos, e só os módulos listados na D10 podem depender de {@code access}.
 */
public enum Role {

    /** Todo login válido novo começa como estudante. */
    STUDENT,

    /** Concedido por um administrador (RF03). */
    STAFF,

    /** Criado na instalação; faz tudo o que o funcionário faz, e mais. */
    ADMIN
}
