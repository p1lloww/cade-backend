package br.ucsal.cade.shared.security;

import org.springframework.security.core.annotation.ExpressionTemplateValueProvider;

import java.util.Set;

/**
 * Perfil de uma conta (RF02). Fica no núcleo compartilhado porque a {@code @RequiresRole} é usada por todos os módulos,
 * e só os listados na D10 podem depender de {@code access}.
 */
public enum Role implements ExpressionTemplateValueProvider {

    STUDENT,
    STAFF,

    /** Faz tudo o que o funcionário faz, mas não herda as rotas do estudante: o administrador não registra perdas. */
    ADMIN(STAFF);

    public static final String AUTHORITY_PREFIX = "ROLE_";

    private final Set<Role> includes;

    Role(Role... includes) {
        this.includes = Set.of(includes);
    }

    public boolean satisfies(Role required) {
        return this == required || includes.contains(required);
    }

    public String authority() {
        return AUTHORITY_PREFIX + name();
    }

    /** Entre aspas porque é inserido na expressão SpEL da {@code @RequiresRole}: {@code hasAnyRole('STAFF')}. */
    @Override
    public String getExpressionTemplateValue() {
        return "'" + name() + "'";
    }
}
