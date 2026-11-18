package br.ucsal.cade.shared.security;

import org.springframework.security.core.annotation.ExpressionTemplateValueProvider;

import java.util.Set;

/**
 * Perfil de uma conta (RF02). Cada conta tem exatamente um perfil; visitantes não têm conta.
 *
 * <p>Fica no núcleo compartilhado porque a autorização por perfil ({@code @RequiresRole}) é usada pelos controllers de
 * todos os módulos, e só os módulos listados na D10 podem depender de {@code access}.
 *
 * <p>Como {@link ExpressionTemplateValueProvider}, cada perfil vira um texto entre aspas ({@code 'STAFF'}) quando o
 * Spring monta a expressão do {@code @PreAuthorize} a partir da {@code @RequiresRole} (D20).
 */
public enum Role implements ExpressionTemplateValueProvider {

    /** Todo login válido novo começa como estudante. */
    STUDENT,

    /** Concedido por um administrador (RF03). */
    STAFF,

    /**
     * Criado na instalação; faz tudo o que o funcionário faz, e mais (seção 2 dos requisitos). Não herda as rotas do
     * estudante: o administrador não registra perdas.
     */
    ADMIN(STAFF);

    /** Prefixo que o Spring Security usa nas authorities de perfil: {@code STAFF} vira {@code ROLE_STAFF}. */
    public static final String AUTHORITY_PREFIX = "ROLE_";

    private final Set<Role> includes;

    Role(Role... includes) {
        this.includes = Set.of(includes);
    }

    /** Se uma conta com este perfil pode usar uma rota que exige {@code required}. */
    public boolean satisfies(Role required) {
        return this == required || includes.contains(required);
    }

    /** A authority do Spring Security correspondente, por exemplo {@code ROLE_STAFF}. */
    public String authority() {
        return AUTHORITY_PREFIX + name();
    }

    /** O perfil como texto SpEL, para {@code hasAnyRole({value})}: {@code STAFF} vira {@code 'STAFF'}. */
    @Override
    public String getExpressionTemplateValue() {
        return "'" + name() + "'";
    }
}
