package br.ucsal.cade.retention.controller.unclaimed;

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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A lista dos não reclamados por caixa (RF39) e o destino dado pelo administrador (RF38). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class UnclaimedItemsControllerTest {

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

    private UUID item;

    @BeforeEach
    void setUp() {
        UUID category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.LOW)).getId();
        item = service.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                LocalDate.now(clock).minusDays(1), "Caixa " + UUID.randomUUID().toString().substring(0, 8), null, "Carteira preta",
                null), UUID.randomUUID()).getId();
        jdbc.update("UPDATE found_items SET status = 'UNCLAIMED' WHERE id = ?", item);
    }

    @Test
    void theStaffListsTheUnclaimedItemsGroupedByBox() throws Exception {
        String box = jdbc.queryForObject("SELECT storage_box FROM found_items WHERE id = ?", String.class, item);

        mvc.perform(get("/unclaimed-items").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.storageBox == '" + box + "')].items[*].id").value(hasItem(item.toString())));
    }

    @Test
    void theAdminGivesTheItemTheBazaarByDefaultAndItLeavesTheList() throws Exception {
        UUID admin = UUID.randomUUID();

        mvc.perform(dispose(item, "{}", admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination").value("BAZAAR"))
                .andExpect(jsonPath("$.destinationLabel").value("Bazar"))
                .andExpect(jsonPath("$.disposedBy").value(admin.toString()))
                .andExpect(jsonPath("$.disposedAt").exists());

        assertThat(jdbc.queryForObject("SELECT status FROM found_items WHERE id = ?", String.class, item))
                .isEqualTo("DISPOSED");
        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = ?",
                String.class, item, "FOUND_ITEM_DISPOSED")).singleElement().asString().contains("Bazar");
        mvc.perform(get("/unclaimed-items").header("Authorization", bearer(Role.STAFF)))
                .andExpect(jsonPath("$[*].items[*].id").value(org.hamcrest.Matchers.not(hasItem(item.toString()))));
    }

    @Test
    void otherNeedsTheNoteAndOnlyAnUnclaimedItemCanBeDisposed() throws Exception {
        mvc.perform(dispose(item, "{\"destination\":\"OTHER\"}", UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        mvc.perform(dispose(item, "{\"destination\":\"OTHER\",\"note\":\"Doado a um projeto social\"}",
                UUID.randomUUID())).andExpect(status().isCreated());

        mvc.perform(dispose(item, "{}", UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Não é possível")));
    }

    @Test
    void onlyTheAdminDisposesAndOnlyStaffListsTheBoxes() throws Exception {
        mvc.perform(post("/unclaimed-items/{id}/disposal", item).header("Authorization", bearer(Role.STAFF))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(get("/unclaimed-items").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/unclaimed-items")).andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder dispose(UUID id, String json, UUID admin) {
        String token = "Bearer " + TestTokens.token(encoder, clock.instant(), claims -> claims.subject(admin.toString())
                .claim(TokenClaims.ROLE, Role.ADMIN.name()));
        return post("/unclaimed-items/{id}/disposal", id).header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }
}
