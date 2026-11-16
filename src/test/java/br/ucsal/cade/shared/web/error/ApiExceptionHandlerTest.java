package br.ucsal.cade.shared.web.error;

import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.TooManyRequestsException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    @RestController
    static class ProbeController {

        record Body(@NotBlank(message = "Informe o nome.") String name) {
        }

        @PostMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException("Regra violada.");
        }

        @PostMapping("/too-many-requests")
        void tooManyRequests() {
            throw new TooManyRequestsException("Aguarde.", Duration.ofMillis(59_001));
        }

        @PostMapping("/validated")
        void validated(@Valid @RequestBody Body body) {
        }

        @PostMapping("/forbidden")
        void forbidden() {
            throw new AccessDeniedException("role STUDENT cannot do this");
        }

        @PostMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("connection refused: postgres:5432");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
            .setControllerAdvice(new ApiExceptionHandler())
            .build();

    @Test
    void turnsAnApiExceptionIntoAProblemDetailWithItsStatus() throws Exception {
        mvc.perform(post("/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Regra violada."));
    }

    @Test
    void addsTheRetryAfterHeaderRoundedUpToTheNextSecond() throws Exception {
        mvc.perform(post("/too-many-requests"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.detail").value("Aguarde."));
    }

    @Test
    void listsTheMessageOfEachInvalidField() throws Exception {
        mvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.INVALID_FIELDS_MESSAGE))
                .andExpect(jsonPath("$.errors.name").value("Informe o nome."));
    }

    @Test
    void turnsADeniedAccessIntoA403ProblemDetail() throws Exception {
        mvc.perform(post("/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.FORBIDDEN_MESSAGE));
    }

    @Test
    void turnsAnUnexpectedErrorIntoA500WithoutLeakingItsDetails() throws Exception {
        mvc.perform(post("/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.UNEXPECTED_MESSAGE))
                .andExpect(content().string(not(containsString("postgres"))));
    }

    @Test
    void refusesAMissingOrMalformedBody() throws Exception {
        mvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.UNREADABLE_BODY_MESSAGE));
    }
}
