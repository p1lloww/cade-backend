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
 * Issues the access token of an authenticated account (D02): the role of the authorization server in OAuth 2.0.
 * The token carries everything the resource server needs, so no database lookup happens on each request; the
 * downside is that a role change only shows up in the next token.
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
