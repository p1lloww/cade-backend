package br.ucsal.cade.shared.security;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The back end as a resource server: which requests get in, and what the token turns into. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class, ResourceServerTest.ProbeController.class})
class ResourceServerTest {

    private static final String SUBJECT = "6f1c2a1e-0000-4000-8000-000000000001";

    /** A protected route that only exists in this test and echoes what Spring Security saw. */
    @RestController
    static class ProbeController {

        @GetMapping("/test/probe")
        Map<String, Object> probe(Authentication authentication) {
            List<String> authorities = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            return Map.of("name", authentication.getName(), "authorities", authorities);
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Test
    void refusesARequestWithoutToken() throws Exception {
        mvc.perform(get("/test/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")));
    }

    @Test
    void acceptsAValidTokenAndTurnsTheRoleIntoAnAuthority() throws Exception {
        String token = token(encoder, claims -> claims.claim(TokenClaims.ROLE, "STAFF"));

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(SUBJECT))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_STAFF"));
    }

    @Test
    void refusesATamperedToken() throws Exception {
        String token = token(encoder, claims -> { });
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"cade-backend\",\"sub\":\"" + SUBJECT + "\",\"role\":\"ADMIN\"}")
                        .getBytes(StandardCharsets.UTF_8));

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesATokenSignedWithAnotherKey() throws Exception {
        JwtEncoder attacker = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(
                "a-different-secret-with-at-least-32-chars".getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + token(attacker, claims -> { })))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesATokenFromAnotherIssuer() throws Exception {
        String token = token(encoder, claims -> claims.issuer("someone-else"));

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsATokenUntilItsExpiryInstantAndRefusesItAfter() throws Exception {
        String token = token(encoder, claims -> { });

        clock.advance(Duration.ofHours(8));
        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        clock.advance(Duration.ofSeconds(1));

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leavesTheAuthRoutesOpen() throws Exception {
        mvc.perform(get("/auth/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    /** A token valid for eight hours from now, as the access module would issue it, with optional changes. */
    private String token(JwtEncoder signer, Consumer<JwtClaimsSet.Builder> changes) {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(TokenClaims.ISSUER)
                .subject(SUBJECT)
                .claim(TokenClaims.ROLE, "STUDENT")
                .issuedAt(clock.instant())
                .expiresAt(clock.instant().plus(Duration.ofHours(8)));
        changes.accept(claims);
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return signer.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
