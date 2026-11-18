package br.ucsal.cade.shared.security;

import br.ucsal.cade.shared.security.jwt.TokenClaims;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;

/** Tokens de acesso para os testes, iguais aos que o módulo de acesso emitiria. */
public final class TestTokens {

    /** O id da conta ({@code sub}) de todo token daqui. */
    public static final String SUBJECT = "6f1c2a1e-0000-4000-8000-000000000001";

    private TestTokens() {
    }

    /** Um token do perfil, válido por oito horas a partir de {@code now}. */
    public static String forRole(JwtEncoder signer, Instant now, Role role) {
        return token(signer, now, claims -> claims.claim(TokenClaims.ROLE, role.name()));
    }

    /** Um token de estudante válido por oito horas a partir de {@code now}, com mudanças opcionais. */
    public static String token(JwtEncoder signer, Instant now, Consumer<JwtClaimsSet.Builder> changes) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(TokenClaims.ISSUER)
                .subject(SUBJECT)
                .claim(TokenClaims.ROLE, Role.STUDENT.name())
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(8)));
        changes.accept(claims);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return signer.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
