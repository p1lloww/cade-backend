package br.ucsal.cade.founditems.service.search;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cada teste usa uma categoria e um bloco só dele, para não esbarrar nos objetos dos outros testes do mesmo banco.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemSearchServiceTest {

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);

    @Autowired
    private FoundItemSearchService service;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private FoundItemRepository repository;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    private UUID category;
    private String block;

    @BeforeEach
    void setUp() {
        category = newCategory();
        block = "Bloco " + UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void theTextFindsTheInternalDescriptionEvenInThePlural() {
        UUID keys = find("Chave com chaveiro vermelho", null);
        find("Garrafa azul", null);

        assertThat(ids(text("chaves vermelhas"))).containsExactly(keys);
    }

    @Test
    void theTextAlsoSearchesTheReservedDetails() {
        UUID phone = find("Celular preto", "Adesivo de estrela na capa");

        assertThat(ids(text("estrela"))).containsExactly(phone);
    }

    @Test
    void allTheWordsMustMatchAndAMinusExcludesAWord() {
        UUID black = find("Fone preto JBL", null);
        UUID white = find("Fone branco JBL", null);

        assertThat(ids(text("fone preto"))).containsExactly(black);
        assertThat(ids(text("fone -preto"))).containsExactly(white);
    }

    @Test
    void aBlankTextDoesNotFilter() {
        UUID one = find("Fone preto", null);
        UUID other = find("Garrafa azul", null);

        assertThat(ids(text("   "))).containsExactlyInAnyOrder(one, other);
    }

    @Test
    void withoutTextTheMostRecentlyFoundComeFirst() {
        UUID older = find("Fone", null, today().minusDays(3));
        UUID newer = find("Fone", null, today());
        UUID middle = find("Fone", null, today().minusDays(1));

        assertThat(ids(text(null))).containsExactly(newer, middle, older);
    }

    @Test
    void filtersByCategory() {
        UUID mine = find("Fone", null);
        UUID otherCategory = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), newCategory(), block,
                "Sala 421", today(), null, null, "Fone", null), UUID.randomUUID()).getId();

        assertThat(ids(new FoundItemSearch(null, category, null, block, null, null)))
                .containsExactly(mine).doesNotContain(otherCategory);
    }

    @Test
    void filtersByTheMonthTheItemWasFound() {
        YearMonth thisMonth = YearMonth.from(today());
        YearMonth lastMonth = thisMonth.minusMonths(1);
        UUID lastDayOfLastMonth = find("Fone", null, lastMonth.atEndOfMonth());
        UUID firstDayOfThisMonth = find("Fone", null, thisMonth.atDay(1));
        find("Fone", null, lastMonth.minusMonths(1).atEndOfMonth());

        assertThat(ids(new FoundItemSearch(null, category, lastMonth, null, null, null)))
                .containsExactly(lastDayOfLastMonth);
        assertThat(ids(new FoundItemSearch(null, category, thisMonth, null, null, null)))
                .containsExactly(firstDayOfThisMonth);
    }

    @Test
    void filtersByBlockAndRoom() {
        UUID room421 = find("Fone", null);
        UUID room300 = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, block, "Sala 300",
                today(), null, null, "Fone", null), UUID.randomUUID()).getId();

        assertThat(ids(new FoundItemSearch(null, null, null, block, null, null)))
                .containsExactlyInAnyOrder(room421, room300);
        assertThat(ids(new FoundItemSearch(null, null, null, block, "Sala 300", null))).containsExactly(room300);
    }

    @Test
    void filtersByOneOrMoreStatuses() {
        UUID available = find("Fone", null);
        UUID returned = find("Fone", null);
        UUID disposed = find("Fone", null);
        jdbc.update("UPDATE found_items SET status = 'RETURNED' WHERE id = ?", returned);
        jdbc.update("UPDATE found_items SET status = 'DISPOSED' WHERE id = ?", disposed);

        assertThat(ids(statuses(List.of(FoundItemStatus.AVAILABLE)))).containsExactly(available);
        assertThat(ids(statuses(List.of(FoundItemStatus.AVAILABLE, FoundItemStatus.RETURNED))))
                .containsExactlyInAnyOrder(available, returned);
        assertThat(ids(statuses(List.of()))).containsExactlyInAnyOrder(available, returned, disposed);
    }

    @Test
    void theFiltersCombine() {
        UUID match = find("Fone preto", null);
        find("Garrafa preta", null);
        UUID otherRoom = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, block, "Sala 300",
                today(), null, null, "Fone preto", null), UUID.randomUUID()).getId();

        assertThat(ids(new FoundItemSearch("fone", category, YearMonth.from(today()), block, "Sala 421",
                List.of(FoundItemStatus.AVAILABLE)))).containsExactly(match).doesNotContain(otherRoom);
    }

    @Test
    void returnsPagesWithTheTotal() {
        for (int i = 0; i < 3; i++) {
            find("Fone", null, today().minusDays(i));
        }

        Page<FoundItem> first = service.search(text(null), PageRequest.of(0, 2));
        Page<FoundItem> second = service.search(text(null), PageRequest.of(1, 2));

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(2);
        assertThat(second.getContent()).hasSize(1);
        assertThat(first.getContent().getFirst().getCategory().getName()).startsWith("Teste ");
    }

    @Test
    void aPageNeverHasMoreThanOneHundredItems() {
        Page<FoundItem> page = service.search(text(null), PageRequest.of(0, 5000));

        assertThat(page.getSize()).isEqualTo(FoundItemSearchService.MAX_PAGE_SIZE);
    }

    @Test
    void theTextSearchUsesTheFullTextIndex() {
        String plan = jdbc.execute((ConnectionCallback<String>) connection -> {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET enable_seqscan = off");
                StringBuilder lines = new StringBuilder();
                try (ResultSet rows = statement.executeQuery("EXPLAIN SELECT f.id FROM found_items f WHERE "
                        + FoundItemRepository.TEXT_VECTOR + " @@ websearch_to_tsquery('portuguese', 'fone')")) {
                    while (rows.next()) {
                        lines.append(rows.getString(1)).append('\n');
                    }
                }
                statement.execute("RESET enable_seqscan");
                return lines.toString();
            }
        });

        assertThat(plan).contains("found_items_text_idx");
    }

    /** RNF02: a busca responde em até 2 s com 1.000 objetos. */
    @Test
    void searchingAmongAThousandItemsTakesLessThanTwoSeconds() {
        insertItems(1000);

        long start = System.nanoTime();
        Page<FoundItem> page = service.search(new FoundItemSearch("fone preto", category, YearMonth.from(today()),
                block, null, List.of(FoundItemStatus.AVAILABLE)), FIRST_PAGE);
        Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

        assertThat(page.getTotalElements()).isEqualTo(500);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    void theOptionsShowOnlyWhatHasItems() {
        YearMonth thisMonth = YearMonth.from(today());
        find("Fone", null, thisMonth.minusMonths(1).atDay(15));
        foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, block, "Sala 300",
                thisMonth.atDay(1), null, null, "Fone", null), UUID.randomUUID());
        UUID unused = newCategory();

        SearchOptions options = service.options();

        assertThat(options.categories()).extracting(Category::getId).contains(category).doesNotContain(unused);
        assertThat(options.places()).contains(new SearchOptions.BlockRooms(block, List.of("Sala 300", "Sala 421")));
        assertThat(options.months()).contains(thisMonth, thisMonth.minusMonths(1));
        assertThat(options.months()).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat(options.statuses()).isEqualTo(FoundItemStatus.ALL);
    }

    private List<UUID> ids(FoundItemSearch search) {
        return service.search(search, FIRST_PAGE).map(FoundItem::getId).getContent();
    }

    /** Só os objetos do bloco do teste. */
    private FoundItemSearch text(String text) {
        return new FoundItemSearch(text, null, null, block, null, null);
    }

    private FoundItemSearch statuses(List<FoundItemStatus> statuses) {
        return new FoundItemSearch(null, null, null, block, null, statuses);
    }

    private UUID find(String description, String reservedDetails) {
        return find(description, reservedDetails, today());
    }

    private UUID find(String description, String reservedDetails, LocalDate foundOn) {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, block, "Sala 421", foundOn,
                null, null, description, reservedDetails), UUID.randomUUID()).getId();
    }

    /** Metade "Fone preto", metade "Garrafa azul", direto no banco para o teste não demorar com as fotos. */
    private void insertItems(int count) {
        List<Object[]> rows = new ArrayList<>();
        Timestamp now = Timestamp.from(clock.instant());
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{UUID.randomUUID(), category, block, "Sala 421", today(), "teste/" + i,
                    "Caixa Teste", RiskLevel.MEDIUM.name(), "AVAILABLE", i % 2 == 0 ? "Fone preto" : "Garrafa azul",
                    UUID.randomUUID(), now});
        }
        jdbc.batchUpdate("""
                INSERT INTO found_items (id, category_id, block, room, found_on, photo_key, storage_box, risk, status,
                                         internal_description, registered_by, registered_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)""", rows);
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
