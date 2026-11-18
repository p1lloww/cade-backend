package br.ucsal.cade.shared.web.openapi;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.authorization.RequiresRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** O Swagger mostra os perfis de cada rota a partir da {@code @RequiresRole}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RoleDocumentationTest.StaffProbeController.class})
class RoleDocumentationTest {

    @RestController
    @RequiresRole(Role.STAFF)
    static class StaffProbeController {

        @GetMapping("/test/docs/staff")
        String staff() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void listsTheAcceptedRolesAndDocumentsTheForbiddenResponse() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/test/docs/staff'].get.description")
                        .value(endsWith("**Perfis:** STAFF, ADMIN")))
                .andExpect(jsonPath("$.paths['/test/docs/staff'].get.responses['403']").exists());
    }

    @Test
    void leavesThePublicRoutesAlone() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/auth/token'].post.responses['403']").doesNotExist());
    }
}
