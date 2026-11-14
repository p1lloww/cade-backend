package br.ucsal.cade.access.service.token;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.persistence.user.User;
import br.ucsal.cade.access.persistence.user.UserRepository;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class AccessTokenIssuerTest {

    @Autowired
    private AccessTokenIssuer issuer;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private UserRepository users;

    @Autowired
    private MutableClock clock;

    @Test
    void issuesASignedTokenWithTheAccountAndRole() {
        User user = users.save(User.newStudent("token@ucsal.edu.br", clock.instant()));

        AccessToken token = issuer.issue(user);
        Jwt jwt = decoder.decode(token.value());

        assertThat(token.expiresIn()).isEqualTo(Duration.ofHours(8));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("cade-backend");
        assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
        assertThat(jwt.getClaimAsString("email")).isEqualTo("token@ucsal.edu.br");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(jwt.getIssuedAt()).isEqualTo(clock.instant());
        assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(8)));
    }

    @Test
    void hidesTheTokenValueInToString() {
        User user = users.save(User.newStudent("segredo@ucsal.edu.br", clock.instant()));

        AccessToken token = issuer.issue(user);

        assertThat(token.toString()).doesNotContain(token.value());
    }
}
