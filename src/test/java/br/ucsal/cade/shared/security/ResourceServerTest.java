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
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O back-end como servidor de recursos: quais requisições entram e no que o token se transforma. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class, ResourceServerTest.ProbeController.class})
class ResourceServerTest {

    /** Uma rota protegida que só existe neste teste e devolve o que o Spring Security enxergou. */
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
    void refusesARequestWithoutTokenWithAProblemDetail() throws Exception {
        mvc.perform(get("/test/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Entre com o seu e-mail institucional para continuar."));
    }

    @Test
    void acceptsAValidTokenAndTurnsTheRoleIntoAnAuthority() throws Exception {
        String token = token(encoder, claims -> claims.claim(TokenClaims.ROLE, "STAFF"));

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(TestTokens.SUBJECT))
                // O Spring Security 7 também acrescenta FACTOR_BEARER (como o usuário se autenticou), sem ordem fixa.
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_STAFF")));
    }

    @Test
    void refusesATamperedToken() throws Exception {
        String token = token(encoder, claims -> { });
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"iss\":\"cade-backend\",\"sub\":\"" + TestTokens.SUBJECT + "\",\"role\":\"ADMIN\"}")
                        .getBytes(StandardCharsets.UTF_8));

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        mvc.perform(get("/test/probe").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", startsWith("Bearer error=\"invalid_token\"")))
                .andExpect(jsonPath("$.status").value(401));
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
    void opensOnlyTheLoginRoutesUnderAuth() throws Exception {
        mvc.perform(post("/auth/login-code").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/auth/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String token(JwtEncoder signer, Consumer<JwtClaimsSet.Builder> changes) {
        return TestTokens.token(signer, clock.instant(), changes);
    }
}
