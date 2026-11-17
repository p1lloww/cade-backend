package br.ucsal.cade.access.service.token;

import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.shared.security.jwt.JwtProperties;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

/**
 * Emite o token de acesso de uma conta autenticada (D02): o papel do servidor de autorização no OAuth 2.0. O token
 * leva tudo de que o servidor de recursos precisa, então nenhuma requisição consulta o banco; a desvantagem é que uma
 * mudança de perfil só aparece no próximo token.
 */
@Component
public class AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    AccessTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public AccessToken issue(User user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(TokenClaims.ISSUER)
                .subject(user.getId().toString())
                .claim(TokenClaims.EMAIL, user.getEmail())
                .claim(TokenClaims.ROLE, user.getRole().name())
                .issuedAt(now)
                .expiresAt(now.plus(properties.ttl()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(value, properties.ttl());
    }
}
