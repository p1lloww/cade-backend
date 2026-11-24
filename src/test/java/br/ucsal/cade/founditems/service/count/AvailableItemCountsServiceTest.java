package br.ucsal.cade.founditems.service.count;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.api.count.AvailableItemCounts;
import br.ucsal.cade.founditems.api.count.CategoryMonthCount;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Uma categoria só do teste, para as contagens não misturarem com os objetos dos outros testes. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class AvailableItemCountsServiceTest {

    @Autowired
    private AvailableItemCounts counts;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Test
    void countsByCategoryAndMonthOnlyWhatIsInTheRoomAndCanBePickedUp() {
        String name = "Teste " + UUID.randomUUID();
        UUID category = categories.saveAndFlush(new Category(name, RiskLevel.MEDIUM)).getId();
        YearMonth thisMonth = YearMonth.from(LocalDate.now(clock));
        LocalDate lastMonth = thisMonth.minusMonths(1).atDay(10);
        find(category, thisMonth.atDay(1));
        UUID unclaimed = find(category, thisMonth.atDay(1));
        UUID reserved = find(category, thisMonth.atDay(1));
        UUID returned = find(category, thisMonth.atDay(1));
        UUID disposed = find(category, thisMonth.atDay(1));
        find(category, lastMonth);
        setStatus(unclaimed, "UNCLAIMED");
        setStatus(reserved, "RESERVED");
        setStatus(returned, "RETURNED");
        setStatus(disposed, "DISPOSED");

        assertThat(counts.byCategoryAndMonth()).filteredOn(row -> row.category().equals(name))
                .containsExactlyInAnyOrder(new CategoryMonthCount(name, thisMonth, 2),
                        new CategoryMonthCount(name, thisMonth.minusMonths(1), 1));
    }

    private UUID find(UUID category, LocalDate foundOn) {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                foundOn, null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    private void setStatus(UUID item, String status) {
        jdbc.update("UPDATE found_items SET status = ? WHERE id = ?", status, item);
    }
}
