package br.ucsal.cade.shared.security.jwt;

/**
 * Contents of the access token that both sides must agree on: the access module writes them when it issues a token
 * and the resource server reads them on every request.
 */
public final class TokenClaims {

    /** Value of the standard {@code iss} claim; a token from any other issuer is refused. */
    public static final String ISSUER = "cade-backend";

    /** Custom claim with the account e-mail. */
    public static final String EMAIL = "email";

    /** Custom claim with the {@link br.ucsal.cade.shared.security.Role} name, e.g. {@code STUDENT}. */
    public static final String ROLE = "role";

    private TokenClaims() {
    }
}
