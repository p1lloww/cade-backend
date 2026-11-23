package br.ucsal.cade.administration.controller.dashboard;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O painel do administrador (RF46): só números, só para o administrador. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theAdminSeesTheCountsByStatusAndCategoryAndNothingAboutThePeople() throws Exception {
        String name = "Cat " + UUID.randomUUID().toString().substring(0, 8);
        UUID category = categories.saveAndFlush(new Category(name, RiskLevel.LOW)).getId();
        for (int i = 0; i < 2; i++) {
            foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                    LocalDate.now(clock).minusDays(1), null, null, "Carteira preta", null), UUID.randomUUID());
        }
        UUID unclaimed = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400",
                "Sala 421", LocalDate.now(clock).minusDays(1), null, null, "Carteira azul", null),
                UUID.randomUUID()).getId();
        jdbc.update("UPDATE found_items SET status = 'UNCLAIMED' WHERE id = ?", unclaimed);

        mvc.perform(get("/dashboard").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatusAndCategory[?(@.category == '" + name + "' && @.status == 'AVAILABLE')]"
                        + ".total").value(2))
                .andExpect(jsonPath("$.byStatusAndCategory[?(@.category == '" + name + "' && @.status == 'UNCLAIMED')]"
                        + ".total").value(1))
                .andExpect(jsonPath("$.unclaimed").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.returnsThisMonth").exists())
                .andExpect(jsonPath("$.generatedAt").exists())
                .andExpect(jsonPath("$.byStatusAndCategory[0].description").doesNotExist());
    }

    @Test
    void onlyTheAdminSeesThePanel() throws Exception {
        for (Role role : new Role[]{Role.STAFF, Role.STUDENT}) {
            mvc.perform(get("/dashboard").header("Authorization", bearer(role))).andExpect(status().isForbidden());
        }
        mvc.perform(get("/dashboard")).andExpect(status().isUnauthorized());
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }
}
