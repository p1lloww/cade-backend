package br.ucsal.cade.shared.web.openapi;

import br.ucsal.cade.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void servesTheDocumentWithoutATokenAndRequiresTheTokenByDefault() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Cadê? API"))
                .andExpect(jsonPath("$.components.securitySchemes['bearer-jwt'].scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0]['bearer-jwt']").exists());
    }

    @Test
    void documentsTheLoginRoutesAsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/auth/login-code'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/auth/login-code'].post.responses['429'].headers['Retry-After']")
                        .exists())
                .andExpect(jsonPath("$.paths['/auth/token'].post.security").value(empty()))
                .andExpect(jsonPath("$.paths['/auth/token'].post.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.TokenRequestDTO.required").exists());
    }

    @Test
    void servesTheSwaggerUiWithoutAToken() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
