package br.ucsal.cade.founditems.controller.search;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Cada teste usa um bloco só dele, para não esbarrar nos objetos dos outros testes. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemSearchControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID category;
    private String block;

    @BeforeEach
    void setUp() {
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
        block = "Bloco " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void theStaffSearchesByTextAndFilters() throws Exception {
        UUID phone = find("Fone preto JBL");
        find("Garrafa azul");

        mvc.perform(search().param("q", "fone").param("categoryId", category.toString())
                        .param("month", YearMonth.from(today()).toString()).param("block", block)
                        .param("room", "Sala 421").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(phone.toString()))
                .andExpect(jsonPath("$.content[0].internalDescription").value("Fone preto JBL"))
                .andExpect(jsonPath("$.content[0].photoPath").value("/found-items/" + phone + "/photo"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void severalStatusesAreSentByRepeatingTheParameter() throws Exception {
        UUID available = find("Fone");
        UUID returned = find("Fone");
        find("Fone");
        jdbc.update("UPDATE found_items SET status = 'RETURNED' WHERE id = ?", returned);
        jdbc.update("UPDATE found_items SET status = 'DISPOSED' WHERE block = ? AND id NOT IN (?, ?)", block,
                available, returned);

        mvc.perform(search().param("block", block).param("status", "AVAILABLE", "RETURNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void returnsThePageAsked() throws Exception {
        for (int i = 0; i < 3; i++) {
            find("Fone");
        }

        mvc.perform(search().param("block", block).param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page.number").value(1))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));
    }

    @Test
    void anUnknownStatusIsABadRequest() throws Exception {
        mvc.perform(search().param("status", "LOST"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMalformedMonthIsABadRequest() throws Exception {
        mvc.perform(search().param("month", "novembro"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMalformedCategoryIsABadRequest() throws Exception {
        mvc.perform(search().param("categoryId", "fone"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theOptionsListWhatHasItems() throws Exception {
        find("Fone");

        mvc.perform(get("/found-items/search-options").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[*].id", hasItem(category.toString())))
                .andExpect(jsonPath("$.places[*].block", hasItem(block)))
                .andExpect(jsonPath("$.months", hasItem(YearMonth.from(today()).toString())))
                .andExpect(jsonPath("$.statuses", hasSize(5)))
                .andExpect(jsonPath("$.statuses[0].code").value("AVAILABLE"))
                .andExpect(jsonPath("$.statuses[0].label").value("Disponível"));
    }

    @Test
    void theAdministratorCanAlsoSearch() throws Exception {
        mvc.perform(get("/found-items").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void aStudentCannotSearch() throws Exception {
        mvc.perform(get("/found-items").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/found-items/search-options").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mvc.perform(get("/found-items"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder search() {
        return get("/found-items").header("Authorization", bearer(Role.STAFF));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private UUID find(String description) {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, block, "Sala 421", today(),
                null, null, description, null), UUID.randomUUID()).getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
