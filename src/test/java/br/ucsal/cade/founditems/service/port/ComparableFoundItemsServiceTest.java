package br.ucsal.cade.founditems.service.port;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.api.candidate.ComparableFoundItem;
import br.ucsal.cade.founditems.api.candidate.ComparableFoundItems;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class ComparableFoundItemsServiceTest {

    @Autowired
    private ComparableFoundItems comparable;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private MutableClock clock;

    @Test
    void returnsTheStaffViewOfEachRequestedItem() {
        Category category = categories.findByNameIgnoreCase("Fone").orElseThrow();
        LocalDate today = LocalDate.now(clock);
        UUID item = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), category.getId(), "Bloco 400",
                "Sala 421", today, null, RiskLevel.HIGH, "Fone preto", "Arranhão na haste"), UUID.randomUUID())
                .getId();

        Map<UUID, ComparableFoundItem> found = comparable.findAll(List.of(item, UUID.randomUUID()));

        assertThat(found).containsOnlyKeys(item);
        ComparableFoundItem view = found.get(item);
        assertThat(view.categoryName()).isEqualTo("Fone");
        assertThat(view.foundOn()).isEqualTo(today);
        assertThat(view.block()).isEqualTo("Bloco 400");
        assertThat(view.room()).isEqualTo("Sala 421");
        assertThat(view.storageBox()).isNotBlank();
        assertThat(view.risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(view.status()).isEqualTo("Disponível");
        assertThat(view.internalDescription()).isEqualTo("Fone preto");
        assertThat(view.reservedDetails()).isEqualTo("Arranhão na haste");
    }

    @Test
    void returnsAnEmptyMapForNoIds() {
        assertThat(comparable.findAll(List.of())).isEmpty();
    }
}
