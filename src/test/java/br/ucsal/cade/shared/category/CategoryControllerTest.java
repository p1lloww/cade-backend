package br.ucsal.cade.shared.category;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CategoryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private DefaultCategoriesInitializer initializer;

    @Test
    void studentsAndStaffListTheDefaultCategoriesWithTheirRisk() throws Exception {
        for (Role role : Role.values()) {
            mvc.perform(get("/categories").header("Authorization",
                            "Bearer " + TestTokens.forRole(encoder, clock.instant(), role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].name", hasItem("Celular")))
                    .andExpect(jsonPath("$[?(@.name == 'Celular')].defaultRisk").value("HIGH"))
                    .andExpect(jsonPath("$[?(@.name == 'Fone')].defaultRiskLabel").value("Médio"))
                    .andExpect(jsonPath("$[?(@.name == 'Documentos')].defaultRisk").value("DOCUMENT"));
        }
    }

    @Test
    void visitorsMustLogIn() throws Exception {
        mvc.perform(get("/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void theDefaultCategoriesAreCreatedOnlyOnce() {
        long before = categories.count();

        initializer.run(null);

        assertThat(categories.count()).isEqualTo(before);
        assertThat(before).isGreaterThanOrEqualTo(DefaultCategoriesInitializer.DEFAULTS.size());
    }
}
