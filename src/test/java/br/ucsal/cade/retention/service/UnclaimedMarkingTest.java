package br.ucsal.cade.retention.service;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.settings.DeadlineSettings;
import br.ucsal.cade.shared.settings.DeadlineSettingsRepository;
import br.ucsal.cade.shared.settings.DeadlineValues;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** A tarefa de não reclamados (RF37, RN16): 90 dias desde a entrada na sala, com relógio controlado. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class UnclaimedMarkingTest {

    private static final Duration STORAGE_PERIOD = Duration.ofDays(90);

    @Autowired
    private UnclaimedMarkingService marking;

    @Autowired
    private FoundItemService service;

    @Autowired
    private FoundItemRepository items;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DeadlineSettingsRepository settings;

    private UUID category;

    @BeforeEach
    void setUp() {
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.LOW)).getId();
    }

    @Test
    void anItemThatPassedTheStoragePeriodBecomesUnclaimedAndGoesToTheHistoryInTheNameOfTheSystem() {
        UUID item = register();

        clock.advance(STORAGE_PERIOD.plusMinutes(1));
        marking.markDueItems();

        assertThat(statusOf(item)).isEqualTo(FoundItemStatus.UNCLAIMED);
        assertThat(jdbc.queryForList("SELECT actor_id FROM audit_records WHERE entity_id = ? AND action = ?",
                UUID.class, item, "FOUND_ITEM_MARKED_UNCLAIMED")).containsExactly(AuditEvent.SYSTEM_ACTOR);
    }

    @Test
    void itBecomesUnclaimedExactlyAtTheDeadlineButNotBefore() {
        UUID item = register();

        clock.advance(STORAGE_PERIOD.minusMinutes(1));
        marking.markDueItems();
        assertThat(statusOf(item)).isEqualTo(FoundItemStatus.AVAILABLE);

        clock.advance(Duration.ofMinutes(1));
        marking.markDueItems();
        assertThat(statusOf(item)).isEqualTo(FoundItemStatus.UNCLAIMED);
    }

    @Test
    void theStoragePeriodIsTheOneTheAdministratorConfigured() {
        UUID item = register();
        jdbc.update("DELETE FROM deadline_settings");
        settings.saveAndFlush(new DeadlineSettings(new DeadlineValues(30, 7, 30), clock.instant(), UUID.randomUUID()));
        try {
            clock.advance(Duration.ofDays(30).plusMinutes(1));
            marking.markDueItems();

            assertThat(statusOf(item)).isEqualTo(FoundItemStatus.UNCLAIMED);
        } finally {
            jdbc.update("DELETE FROM deadline_settings");
        }
    }

    @Test
    void aReservedItemKeepsItsReservationAndRunningTwiceMarksOnlyOnce() {
        UUID reserved = register();
        UUID available = register();
        jdbc.update("UPDATE found_items SET status = 'RESERVED' WHERE id = ?", reserved);

        clock.advance(STORAGE_PERIOD.plusDays(1));
        marking.markDueItems();
        marking.markDueItems();

        assertThat(statusOf(reserved)).isEqualTo(FoundItemStatus.RESERVED);
        assertThat(statusOf(available)).isEqualTo(FoundItemStatus.UNCLAIMED);
        assertThat(jdbc.queryForList("SELECT action FROM audit_records WHERE entity_id = ?", String.class, available))
                .containsOnlyOnce("FOUND_ITEM_MARKED_UNCLAIMED");
    }

    private UUID register() {
        return service.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                LocalDate.now(clock).minusDays(1), null, null, "Carteira preta", null), UUID.randomUUID()).getId();
    }

    private FoundItemStatus statusOf(UUID id) {
        return items.findById(id).orElseThrow().getStatus();
    }
}
