package br.ucsal.cade.shared.security.authorization;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class,
        RequiresRoleTest.StaffProbeController.class})
class RequiresRoleTest {

    @RestController
    @RequiresRole(Role.STAFF)
    static class StaffProbeController {

        @GetMapping("/test/roles/staff")
        String staff() {
            return "ok";
        }

        @GetMapping("/test/roles/student")
        @RequiresRole(Role.STUDENT)
        String student() {
            return "ok";
        }

        @GetMapping("/test/roles/admin")
        @RequiresRole(Role.ADMIN)
        String admin() {
            return "ok";
        }
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @ParameterizedTest(name = "{0} em {1}: {2}")
    @CsvSource({
            "STUDENT, /test/roles/student, 200",
            "STUDENT, /test/roles/staff,   403",
            "STUDENT, /test/roles/admin,   403",
            "STAFF,   /test/roles/student, 403",
            "STAFF,   /test/roles/staff,   200",
            "STAFF,   /test/roles/admin,   403",
            "ADMIN,   /test/roles/student, 403",
            "ADMIN,   /test/roles/staff,   200",
            "ADMIN,   /test/roles/admin,   200"})
    void letsInOnlyTheRolesTheRouteAccepts(Role role, String route, int expectedStatus) throws Exception {
        mvc.perform(get(route).header("Authorization", "Bearer " + TestTokens.forRole(encoder, clock.instant(), role)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void refusesTheWrongRoleWithAProblemDetailInPortuguese() throws Exception {
        String token = TestTokens.forRole(encoder, clock.instant(), Role.STUDENT);

        mvc.perform(get("/test/roles/staff").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Você não tem permissão para fazer isso."));
    }

    @Test
    void stillAsksForTheTokenBeforeLookingAtTheRole() throws Exception {
        mvc.perform(get("/test/roles/student"))
                .andExpect(status().isUnauthorized());
    }
}
