package br.ucsal.cade.access.controller.auth;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.access.api.event.LoginCodeRequestedEvent;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class AuthControllerTest {

    private static final String INVALID_CODE = "Código inválido ou expirado. Confira o código ou solicite um novo.";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @Test
    void exchangesTheEmailedCodeForAStudentTokenValidForEightHours() throws Exception {
        String code = requestCode("fluxo@ucsal.edu.br");

        String body = exchange("fluxo@ucsal.edu.br", code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(Duration.ofHours(8).toSeconds()))
                .andReturn().getResponse().getContentAsString();

        Jwt jwt = decoder.decode(JsonPath.read(body, "$.accessToken"));
        assertThat(jwt.getClaimAsString("email")).isEqualTo("fluxo@ucsal.edu.br");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("STUDENT");
        assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(8)));
    }

    @Test
    void theIssuedTokenOpensProtectedRoutes() throws Exception {
        String code = requestCode("acesso@ucsal.edu.br");
        String body = exchange("acesso@ucsal.edu.br", code).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.accessToken");

        mvc.perform(get("/protected/does-not-exist"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/protected/does-not-exist").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptsTheEmailInAnyCaseAndWithSpaces() throws Exception {
        String code = requestCode(" Caixa@UCSAL.edu.br ", "caixa@ucsal.edu.br");

        exchange("CAIXA@ucsal.edu.br", code)
                .andExpect(status().isOk());
    }

    @Test
    void refusesAnEmailOutsideTheInstitutionalDomain() throws Exception {
        askForCode("aluno@gmail.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Use o seu e-mail institucional @ucsal.edu.br."));
    }

    @Test
    void refusesAMalformedEmail() throws Exception {
        askForCode("not-an-email")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("Informe um e-mail válido."));
    }

    @Test
    void asksToWaitBeforeSendingAnotherCode() throws Exception {
        requestCode("espera@ucsal.edu.br");

        String retryAfter = askForCode("espera@ucsal.edu.br")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail")
                        .value("Um código acabou de ser enviado. Aguarde um minuto para pedir outro."))
                .andReturn().getResponse().getHeader("Retry-After");

        assertThat(Long.parseLong(retryAfter)).isBetween(1L, 60L);
    }

    @Test
    void refusesACodeOutsideTheSixDigitFormat() throws Exception {
        exchange("formato@ucsal.edu.br", "12ab")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.code").value("O código tem 6 dígitos."));
    }

    @Test
    void refusesAReusedCode() throws Exception {
        String code = requestCode("reuso@ucsal.edu.br");
        exchange("reuso@ucsal.edu.br", code).andExpect(status().isOk());

        exchange("reuso@ucsal.edu.br", code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID_CODE));
    }

    @Test
    void refusesAnExpiredCode() throws Exception {
        String code = requestCode("expirado@ucsal.edu.br");
        clock.advance(Duration.ofMinutes(10));

        exchange("expirado@ucsal.edu.br", code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID_CODE));
    }

    @Test
    void locksTheCodeAfterFiveWrongAttempts() throws Exception {
        String code = requestCode("bloqueio@ucsal.edu.br");
        String wrong = code.equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            exchange("bloqueio@ucsal.edu.br", wrong).andExpect(status().isBadRequest());
        }

        exchange("bloqueio@ucsal.edu.br", code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID_CODE));
    }

    private String requestCode(String email) throws Exception {
        return requestCode(email, email);
    }

    /** Pede um código e lê o valor puro no evento publicado, como faria quem envia o e-mail. */
    private String requestCode(String typedEmail, String normalizedEmail) throws Exception {
        askForCode(typedEmail).andExpect(status().isAccepted());
        return events.stream(LoginCodeRequestedEvent.class)
                .filter(event -> event.email().equals(normalizedEmail))
                .reduce((first, second) -> second)
                .orElseThrow()
                .code();
    }

    private ResultActions askForCode(String email) throws Exception {
        return mvc.perform(post("/auth/login-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions exchange(String email, String code) throws Exception {
        return mvc.perform(post("/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"));
    }
}
