package br.ucsal.cade.publicpage.controller.counts;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Uma categoria só do teste, para achar as contagens dele entre as dos outros testes. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class PublicCountsControllerTest {

    private static final String DESCRIPTION = "Garrafa térmica azul Stanley";
    private static final String RESERVED = "Adesivo de caveira no fundo";
    private static final String BLOCK = "Bloco Secreto";
    private static final String ROOM = "Sala 999";
    private static final String BOX = "Caixa Secreta";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MutableClock clock;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private CategoryRepository categories;

    private String category;
    private UUID categoryId;

    @BeforeEach
    void setUp() {
        category = "Teste " + UUID.randomUUID();
        categoryId = categories.saveAndFlush(new Category(category, RiskLevel.MEDIUM)).getId();
    }

    @Test
    void anyoneSeesTheCountsWithoutLoggingIn() throws Exception {
        find();
        find();
        YearMonth month = YearMonth.from(today());

        String body = mvc.perform(get(PublicCountsController.PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<Map<String, Object>> mine = JsonPath.read(body,
                "$.months[?(@.month == '" + month + "')].categories[?(@.category == '" + category + "')]");
        assertThat(mine).singleElement().satisfies(row -> assertThat(row.get("count")).isEqualTo(2));
        String name = month.getMonth().getDisplayName(TextStyle.FULL, Locale.of("pt", "BR"));
        assertThat(JsonPath.<List<String>>read(body, "$.months[?(@.month == '" + month + "')].label"))
                .containsExactly(Character.toUpperCase(name.charAt(0)) + name.substring(1) + "/" + month.getYear());
    }

    /** RN03: só contagens saem; nada que permita reconhecer o objeto. */
    @Test
    void neverShowsPhotoDescriptionOrPlace() throws Exception {
        UUID item = find();

        String body = mvc.perform(get(PublicCountsController.PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(DESCRIPTION, "Stanley", RESERVED, "caveira", BLOCK, ROOM, BOX,
                item.toString(), categoryId.toString(), "photo", "description", "block", "room");
        assertThat(JsonPath.<Map<String, Object>>read(body, "$").keySet()).containsExactly("months");
        assertThat(JsonPath.<List<Map<String, Object>>>read(body, "$.months[*]"))
                .allSatisfy(month -> assertThat(month.keySet())
                        .isEqualTo(Set.of("month", "label", "total", "categories")));
        assertThat(JsonPath.<List<Map<String, Object>>>read(body, "$.months[*].categories[*]"))
                .allSatisfy(row -> assertThat(row.keySet()).isEqualTo(Set.of("category", "count")));
    }

    private UUID find() {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), categoryId, BLOCK, ROOM, today(), BOX,
                null, DESCRIPTION, RESERVED), UUID.randomUUID()).getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
