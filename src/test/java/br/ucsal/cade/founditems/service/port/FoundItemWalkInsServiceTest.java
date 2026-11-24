package br.ucsal.cade.founditems.service.port;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.api.reservation.FoundItemWalkIns;
import br.ucsal.cade.founditems.api.reservation.WalkInFoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.exception.NotFoundException;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemWalkInsServiceTest {

    @Autowired
    private FoundItemWalkIns walkIns;

    @Autowired
    private FoundItemService service;

    @Autowired
    private FoundItemRepository items;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private TransactionTemplate transaction;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    private UUID category;
    private UUID staff;

    @BeforeEach
    void setUp() {
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.HIGH)).getId();
        staff = UUID.randomUUID();
    }

    @Test
    void showsTheRiskAndWhetherTheItemCanBeHandedOver() {
        UUID item = register();

        WalkInFoundItem view = walkIns.find(item);

        assertThat(view.risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(view.statusLabel()).isEqualTo("Disponível");
        assertThat(view.handOverWithoutSummons()).isTrue();
    }

    @Test
    void handsOverAnAvailableOrUnclaimedItemWithHistory() {
        UUID available = register();
        UUID unclaimed = register();
        jdbc.update("UPDATE found_items SET status = 'UNCLAIMED' WHERE id = ?", unclaimed);

        transaction.executeWithoutResult(_ -> {
            walkIns.handOverWithoutSummons(available, staff);
            walkIns.handOverWithoutSummons(unclaimed, staff);
        });

        assertThat(items.findById(available).orElseThrow().getStatus()).isEqualTo(FoundItemStatus.RETURNED);
        assertThat(items.findById(unclaimed).orElseThrow().getStatus()).isEqualTo(FoundItemStatus.RETURNED);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE entity_id = ? AND action = ? "
                        + "AND actor_id = ?", Long.class, available, FoundItemWalkInsService.HANDED_OVER_ACTION, staff))
                .isOne();
        assertThat(walkIns.find(available).handOverWithoutSummons()).isFalse();
    }

    @Test
    void refusesAReservedItemBecauseItIsPromisedToASummonedStudent() {
        UUID item = register();
        jdbc.update("UPDATE found_items SET status = 'RESERVED' WHERE id = ?", item);

        assertThatThrownBy(() -> transaction.executeWithoutResult(_ -> walkIns.handOverWithoutSummons(item, staff)))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Não é possível entregar sem convocação um objeto na situação Reservado.");
        assertThat(items.findById(item).orElseThrow().getStatus()).isEqualTo(FoundItemStatus.RESERVED);
        assertThat(walkIns.find(item).handOverWithoutSummons()).isFalse();
    }

    @Test
    void anUnknownItemIsNotFound() {
        assertThatThrownBy(() -> walkIns.find(UUID.randomUUID())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void theHandOverOnlyRunsInsideTheTransactionOfThePickup() {
        UUID item = register();

        assertThatThrownBy(() -> walkIns.handOverWithoutSummons(item, staff))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private UUID register() {
        return service.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 300", "Sala 305",
                LocalDate.now(clock), null, null, "iPhone preto", null), UUID.randomUUID()).getId();
    }
}
