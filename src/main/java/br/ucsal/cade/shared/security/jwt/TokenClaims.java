package br.ucsal.cade.shared.security.jwt;

/**
 * Conteúdo do token de acesso em que os dois lados precisam concordar: o módulo de acesso escreve ao emitir o token,
 * e o servidor de recursos lê em toda requisição.
 */
public final class TokenClaims {

    /** Valor da claim padrão {@code iss}; um token de qualquer outro emissor é recusado. */
    public static final String ISSUER = "cade-backend";

    /** Claim própria com o e-mail da conta. */
    public static final String EMAIL = "email";

    /** Claim própria com o nome do {@link br.ucsal.cade.shared.security.Role}, por exemplo {@code STUDENT}. */
    public static final String ROLE = "role";

    private TokenClaims() {
    }
}
