package br.ucsal.cade.administration.controller.category;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O administrador gerencia as categorias: nome, risco padrão e perguntas de conferência (RF44). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CategoryAdminControllerTest {

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

    @Test
    void theAdminCreatesACategoryAndTheStaffSeesItWithItsQuestions() throws Exception {
        String name = uniqueName();

        ResultActions created = mvc.perform(json(post("/categories"), Role.ADMIN, """
                {"name": "  %s ", "defaultRisk": "HIGH",
                 "verificationQuestions": ["Qual a cor da capa?", " Tem adesivo? "]}""".formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.defaultRisk").value("HIGH"))
                .andExpect(jsonPath("$.verificationQuestions", contains("Qual a cor da capa?", "Tem adesivo?")));
        String id = JsonPath.read(created.andReturn().getResponse().getContentAsString(), "$.id");

        mvc.perform(get("/categories").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + name + "')].verificationQuestions[0]")
                        .value("Qual a cor da capa?"));
        assertThat(jdbc.queryForList("SELECT action FROM audit_records WHERE entity_id = ?", String.class,
                UUID.fromString(id))).containsExactly("CATEGORY_CREATED");
    }

    @Test
    void theAdminChangesNameRiskAndQuestionsAndTheHistoryKeepsWhatChanged() throws Exception {
        Category category = categories.saveAndFlush(new Category(uniqueName(), RiskLevel.LOW));
        String newName = uniqueName();

        mvc.perform(json(put("/categories/{id}", category.getId()), Role.ADMIN, """
                {"name": "%s", "defaultRisk": "MEDIUM", "verificationQuestions": ["Qual a marca?"]}"""
                .formatted(newName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName))
                .andExpect(jsonPath("$.defaultRisk").value("MEDIUM"))
                .andExpect(jsonPath("$.verificationQuestions[0]").value("Qual a marca?"));

        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = ?",
                String.class, category.getId(), "CATEGORY_UPDATED")).singleElement().asString()
                .contains("nome").contains("risco Baixo → Médio").contains("perguntas de conferência (1)");
    }

    @Test
    void aDuplicateNameIsAConflictEvenWithDifferentCaseAndAnUnknownCategoryIsNotFound() throws Exception {
        Category existing = categories.saveAndFlush(new Category(uniqueName(), RiskLevel.LOW));

        mvc.perform(json(post("/categories"), Role.ADMIN, """
                {"name": "%s", "defaultRisk": "LOW"}""".formatted(existing.getName().toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Já existe uma categoria")));
        mvc.perform(json(put("/categories/{id}", UUID.randomUUID()), Role.ADMIN, """
                {"name": "%s", "defaultRisk": "LOW"}""".formatted(uniqueName()))).andExpect(status().isNotFound());
    }

    @Test
    void blankQuestionsAndMissingFieldsAreRefused() throws Exception {
        mvc.perform(json(post("/categories"), Role.ADMIN, """
                {"name": "%s", "defaultRisk": "LOW", "verificationQuestions": ["  "]}""".formatted(uniqueName())))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/categories"), Role.ADMIN, "{\"name\": \"%s\"}".formatted(uniqueName())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyTheAdminManagesCategoriesButStaffAndStudentsStillListThem() throws Exception {
        mvc.perform(json(post("/categories"), Role.STAFF, "{\"name\":\"x\",\"defaultRisk\":\"LOW\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(json(put("/categories/{id}", UUID.randomUUID()), Role.STUDENT,
                "{\"name\":\"x\",\"defaultRisk\":\"LOW\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/categories").header("Authorization", bearer(Role.STAFF))).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, Role role,
            String body) {
        return request.header("Authorization", bearer(role)).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private static String uniqueName() {
        return "Cat " + UUID.randomUUID().toString().substring(0, 8);
    }
}
