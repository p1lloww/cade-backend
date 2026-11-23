package br.ucsal.cade.administration.controller.settings;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O administrador ajusta os prazos de guarda, de retirada e de validade do registro (RF45). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class DeadlineSettingsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    /** Os outros testes contam com os prazos iniciais: cada teste começa e termina sem ajuste gravado. */
    @BeforeEach
    @AfterEach
    void resetSettings() {
        jdbc.update("DELETE FROM deadline_settings");
    }

    @Test
    void withoutAnyAdjustmentTheInitialDeadlinesApplyAndTheAdminChangesThemAtOnce() throws Exception {
        mvc.perform(get("/settings/deadlines").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageDays").value(90))
                .andExpect(jsonPath("$.summonsDays").value(7))
                .andExpect(jsonPath("$.reportValidityDays").value(30));

        mvc.perform(update("{\"storageDays\":120,\"summonsDays\":5,\"reportValidityDays\":30}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageDays").value(120))
                .andExpect(jsonPath("$.summonsDays").value(5));
        mvc.perform(get("/settings/deadlines").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(jsonPath("$.storageDays").value(120))
                .andExpect(jsonPath("$.reportValidityDays").value(30));

        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE action = ? ORDER BY occurred_at DESC",
                String.class, "DEADLINES_UPDATED")).first().asString()
                .contains("guarda 90 → 120 dias").contains("retirada 7 → 5 dias").doesNotContain("validade");
    }

    @Test
    void deadlinesOutsideTheLimitsOrMissingAreRefusedAndNothingIsSaved() throws Exception {
        mvc.perform(update("{\"storageDays\":10,\"summonsDays\":7,\"reportValidityDays\":30}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("entre 30 e 365")));
        mvc.perform(update("{\"storageDays\":90,\"summonsDays\":0,\"reportValidityDays\":30}"))
                .andExpect(status().isBadRequest());
        mvc.perform(update("{\"storageDays\":90,\"summonsDays\":7}")).andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM deadline_settings", Long.class)).isZero();
    }

    @Test
    void aNewLossReportUsesTheConfiguredValidity() throws Exception {
        mvc.perform(update("{\"storageDays\":90,\"summonsDays\":7,\"reportValidityDays\":10}"))
                .andExpect(status().isOk());
        UUID category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.LOW)).getId();
        String student = "Bearer " + TestTokens.token(encoder, clock.instant(), claims ->
                claims.subject(UUID.randomUUID().toString()).claim(TokenClaims.ROLE, Role.STUDENT.name()));

        mvc.perform(multipart("/loss-reports").file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE,
                        """
                        {"categoryId": "%s", "description": "Garrafa azul", "lostFrom": "%s"}"""
                                .formatted(category, LocalDate.now(clock).minusDays(1))
                                .getBytes(StandardCharsets.UTF_8)))
                .header("Authorization", student)).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT max(EXTRACT(EPOCH FROM (expires_at - registered_at))) FROM loss_reports"
                + " WHERE category_id = ?", Long.class, category)).isEqualTo(10L * 24 * 3600);
    }

    @Test
    void onlyTheAdminSeesAndChangesTheDeadlines() throws Exception {
        for (Role role : new Role[]{Role.STAFF, Role.STUDENT}) {
            mvc.perform(get("/settings/deadlines").header("Authorization", bearer(role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/settings/deadlines")).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder update(String body) {
        return put("/settings/deadlines").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }
}
