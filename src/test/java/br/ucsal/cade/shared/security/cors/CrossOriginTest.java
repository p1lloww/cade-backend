package br.ucsal.cade.shared.security.cors;

import br.ucsal.cade.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O navegador do front-end conversando com a API: o preflight e os headers de resposta. */
@SpringBootTest(properties = "cade.cors.allowed-origins=https://front.example, https://other-front.example")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CrossOriginTest {

    private static final String FRONT = "https://front.example";

    @Autowired
    private MockMvc mvc;

    @Test
    void answersThePreflightOfAProtectedRouteWithoutAToken() throws Exception {
        mvc.perform(options("/auth/me")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "authorization, " + CrossOriginConfiguration.NGROK_SKIP_WARNING))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString(CrossOriginConfiguration.NGROK_SKIP_WARNING)))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void acceptsEveryConfiguredOrigin() throws Exception {
        mvc.perform(options("/auth/token")
                        .header(HttpHeaders.ORIGIN, "https://other-front.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://other-front.example"));
    }

    @Test
    void refusesAnUnknownOrigin() throws Exception {
        mvc.perform(options("/auth/token")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void refusesAHeaderOutsideTheList() throws Exception {
        mvc.perform(options("/auth/token")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-custom"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exposesRetryAfterToTheFrontEnd() throws Exception {
        mvc.perform(post("/auth/token")
                        .header(HttpHeaders.ORIGIN, FRONT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        containsString(HttpHeaders.RETRY_AFTER)));
    }
}
