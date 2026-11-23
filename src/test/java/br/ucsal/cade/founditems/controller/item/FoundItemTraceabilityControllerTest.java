package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quem trouxe o objeto (RF07) e a marcação de objeto identificado (RF08): só funcionários veem. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemTraceabilityControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private FoundItemService service;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID staff;
    private UUID category;
    private UUID item;

    @BeforeEach
    void setUp() {
        staff = UUID.randomUUID();
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
        item = service.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                LocalDate.now(clock).minusDays(1), null, null, "Carteira preta", null), staff).getId();
    }

    @Test
    void whoBroughtTheItemCanBeRecordedChangedAndClearedWithoutTheNameInTheHistory() throws Exception {
        mvc.perform(put("/found-items/{id}/brought-by", item).header("Authorization", staffBearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"broughtBy\":\"  Maria Souza (202012345) \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broughtBy").value("Maria Souza (202012345)"));
        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = ?",
                String.class, item, "FOUND_ITEM_BROUGHT_BY_RECORDED")).singleElement()
                .asString().doesNotContain("Maria").doesNotContain("2020");

        mvc.perform(put("/found-items/{id}/brought-by", item).header("Authorization", staffBearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"broughtBy\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.broughtBy").doesNotExist());
    }

    @Test
    void theItemIsMarkedAsIdentifiedAndOnlyTheFieldNamesGoToTheHistory() throws Exception {
        mvc.perform(identify(item, "{\"name\":\" Maria Souza \",\"email\":\"Maria.Souza@UCSAL.edu.br\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identified").value(true))
                .andExpect(jsonPath("$.identifiedName").value("Maria Souza"))
                .andExpect(jsonPath("$.identifiedEmail").value("maria.souza@ucsal.edu.br"))
                .andExpect(jsonPath("$.identifiedDocument").doesNotExist())
                .andExpect(jsonPath("$.identifiedAt").exists());

        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = ?",
                String.class, item, "FOUND_ITEM_IDENTIFIED")).singleElement()
                .asString().contains("nome, e-mail").doesNotContain("Maria").doesNotContain("ucsal");
    }

    @Test
    void aReservedItemCanBeIdentifiedButADeliveredOrDisposedOneCannot() throws Exception {
        jdbc.update("UPDATE found_items SET status = 'RESERVED' WHERE id = ?", item);
        mvc.perform(identify(item, "{\"name\":\"Maria\"}")).andExpect(status().isOk());

        for (String finished : new String[]{"RETURNED", "DISPOSED"}) {
            jdbc.update("UPDATE found_items SET status = ? WHERE id = ?", finished, item);
            mvc.perform(identify(item, "{\"name\":\"João\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(startsWith("Não é possível identificar um objeto")));
        }
    }

    @Test
    void onlyStaffSeesAndChangesTheseData() throws Exception {
        String student = "Bearer " + TestTokens.forRole(encoder, clock.instant(), Role.STUDENT);
        mvc.perform(put("/found-items/{id}/identification", item).header("Authorization", student)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/found-items/{id}/brought-by", item).header("Authorization", student)
                .contentType(MediaType.APPLICATION_JSON).content("{\"broughtBy\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/found-items/{id}/identification", item).header("Authorization", student))
                .andExpect(status().isForbidden());
        mvc.perform(put("/found-items/{id}/identification", item).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Maria\"}")).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/found-items/{id}", item)
                .header("Authorization", student)).andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder identify(UUID id, String json) {
        return put("/found-items/{id}/identification", id).header("Authorization", staffBearer())
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String staffBearer() {
        return "Bearer " + TestTokens.token(encoder, clock.instant(), claims -> claims.subject(staff.toString())
                .claim(TokenClaims.ROLE, Role.STAFF.name()));
    }
}
