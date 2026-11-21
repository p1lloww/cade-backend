package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.matching.exception.SuggestionNotFoundException;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.matching.service.score.result.CompatibilityBand;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os objetos e as perdas ficam em categorias criadas só para cada teste, para que o cruzamento automático disparado
 * pelos cadastros não grave sugestões além das que o teste cria.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class SuggestionServiceTest {

    @Autowired
    private SuggestionService service;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    private UUID itemCategory;
    private UUID reportCategory;

    @BeforeEach
    void setUp() {
        itemCategory = newCategory();
        reportCategory = newCategory();
    }

    @Test
    void listsTheActiveSuggestionsOfAnItemFromTheHighestScore() {
        UUID item = find();
        UUID medium = lose("Fone preto");
        UUID high = lose("Fone JBL preto");
        UUID discarded = lose("Fone branco");
        suggest(item, medium, 45);
        suggest(item, high, 80);
        MatchSuggestion old = suggest(item, discarded, 90);
        old.discard();
        suggestions.saveAndFlush(old);

        List<ComparedSuggestion> listed = service.forFoundItem(item);

        assertThat(listed).extracting(compared -> compared.lossReport().id()).containsExactly(high, medium);
        assertThat(listed).extracting(ComparedSuggestion::band)
                .containsExactly(CompatibilityBand.HIGH, CompatibilityBand.MEDIUM);
        assertThat(listed.getFirst().foundItem().id()).isEqualTo(item);
        assertThat(listed.getFirst().foundItem().internalDescription()).isEqualTo("Fone preto");
        assertThat(listed.getFirst().lossReport().description()).isEqualTo("Fone JBL preto");
        assertThat(listed.getFirst().lossReport().secretDetail()).isEqualTo("Adesivo de estrela");
    }

    @Test
    void listsTheSuggestionsOfAReportFromTheHighestScore() {
        UUID report = lose("Fone JBL preto");
        UUID low = find();
        UUID high = find();
        suggest(low, report, 40);
        suggest(high, report, 75);

        assertThat(service.forLossReport(report)).extracting(compared -> compared.foundItem().id())
                .containsExactly(high, low);
    }

    @Test
    void anItemWithoutSuggestionsListsNothing() {
        assertThat(service.forFoundItem(find())).isEmpty();
        assertThat(service.forLossReport(UUID.randomUUID())).isEmpty();
    }

    @Test
    void discardsANewSuggestionAndRecordsTheReason() {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);
        UUID staff = UUID.randomUUID();

        ComparedSuggestion discarded = service.discard(suggestion.getId(), "A cor não bate com a foto.", staff);

        assertThat(discarded.suggestion().getStatus()).isEqualTo(SuggestionStatus.DISCARDED);
        MatchSuggestion saved = suggestions.findById(suggestion.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(saved.getVersion()).isEqualTo(suggestion.getVersion() + 1);
        assertThat(jdbc.queryForObject("""
                SELECT reason FROM audit_records
                WHERE entity_id = ? AND actor_id = ? AND action = ? AND entity_type = 'MatchSuggestion'""",
                String.class, suggestion.getId(), staff, SuggestionService.DISCARDED_ACTION))
                .isEqualTo("A cor não bate com a foto.");
        assertThat(service.forFoundItem(suggestion.getFoundItemId())).isEmpty();
    }

    @Test
    void refusesToDiscardASuggestionThatIsNoLongerNew() {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);
        service.discard(suggestion.getId(), "Primeiro descarte.", UUID.randomUUID());

        assertThatThrownBy(() -> service.discard(suggestion.getId(), "Segundo descarte.", UUID.randomUUID()))
                .isInstanceOf(InvalidStatusTransitionException.class);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE entity_id = ? AND action = ?",
                Long.class, suggestion.getId(), SuggestionService.DISCARDED_ACTION)).isOne();
    }

    @Test
    void anUnknownSuggestionIsNotFound() {
        assertThatThrownBy(() -> service.find(UUID.randomUUID())).isInstanceOf(SuggestionNotFoundException.class);
        assertThatThrownBy(() -> service.discard(UUID.randomUUID(), "Motivo.", UUID.randomUUID()))
                .isInstanceOf(SuggestionNotFoundException.class);
    }

    @Test
    void findsASuggestionInAnyStatus() {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);
        service.discard(suggestion.getId(), "Não é o mesmo.", UUID.randomUUID());

        ComparedSuggestion found = service.find(suggestion.getId());

        assertThat(found.suggestion().getStatus()).isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(found.foundItem().id()).isEqualTo(suggestion.getFoundItemId());
        assertThat(found.lossReport().id()).isEqualTo(suggestion.getLossReportId());
    }

    private MatchSuggestion suggest(UUID item, UUID report, int score) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, score, clock.instant()));
    }

    private UUID find() {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400", "Sala 421",
                today(), null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    private UUID lose(String description) {
        return lossReports.register(new RegisterLossReport(reportCategory, description, "Adesivo de estrela",
                List.of(LossLocation.of("Bloco 400", "Sala 421")), today(), null, null, null), UUID.randomUUID())
                .getId();
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
