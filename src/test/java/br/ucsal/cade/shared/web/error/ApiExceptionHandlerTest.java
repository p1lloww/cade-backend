package br.ucsal.cade.shared.web.error;

import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.TooManyRequestsException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

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
    void refusesAMissingOrMalformedBody() throws Exception {
        mvc.perform(post("/validated").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ApiExceptionHandler.UNREADABLE_BODY_MESSAGE));
    }
}
