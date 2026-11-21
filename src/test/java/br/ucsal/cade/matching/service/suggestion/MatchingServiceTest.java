package br.ucsal.cade.matching.service.suggestion;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.audit.AuditEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * O cruzamento disparado pelos cadastros, de ponta a ponta (RF20, D05): o cadastro publica o evento, o ouvinte roda
 * depois do commit numa Virtual Thread e grava as sugestões médias e altas. Como o cruzamento é assíncrono, os testes
 * esperam a sugestão aparecer no banco.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class MatchingServiceTest {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @MockitoSpyBean
    private MatchingService matching;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private FoundItemRepository items;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private MutableClock clock;

    @Test
    void suggestsTheRn07ExampleWhenTheItemArrivesAfterTheLoss() {
        UUID report = lose(category("Fone"), "Fone JBL preto", "Bloco 400", "Sala 421", today());
        UUID item = find("Fone preto", "Bloco 400", "Sala 421", today());

        MatchSuggestion suggestion = awaitSuggestion(item, report);

        assertThat(suggestion.getScore()).isEqualTo(80);
        assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.NEW);
        assertThat(suggestion.getCreatedAt()).isEqualTo(clock.instant());
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM audit_records
                WHERE entity_id = ? AND actor_id = ? AND action = ? AND entity_type = 'MatchSuggestion'""",
                Long.class, suggestion.getId(), AuditEvent.SYSTEM_ACTOR, MatchingService.SUGGESTED_ACTION)).isOne();
    }

    @Test
    void suggestsThePairWhenTheLossArrivesAfterTheItem() {
        UUID item = find("Fone preto", "Bloco 400", "Sala 421", today());
        UUID report = lose(category("Fone"), "Fone JBL preto", "Bloco 400", "Sala 421", today());

        assertThat(awaitSuggestion(item, report).getScore()).isEqualTo(80);
    }

    @Test
    void keepsOnlyMediumAndHighSuggestionsOfTheSameCategory() {
        UUID low = lose(category("Fone"), "Capa azul", "Bloco 900", null, today().minusDays(20));
        UUID otherCategory = lose(category("Celular"), "Capa vermelha", "Bloco 100", "Sala 1", today());
        UUID medium = lose(category("Fone"), "Capa vermelha", "Bloco 900", null, today());
        UUID item = find(null, "Bloco 100", "Sala 1", today());

        // Data no mesmo dia (40), outro bloco e nenhuma palavra: média.
        assertThat(awaitSuggestion(item, medium).getScore()).isEqualTo(40);
        assertThat(suggestions.findByFoundItemIdAndLossReportId(item, low)).isEmpty();
        assertThat(suggestions.findByFoundItemIdAndLossReportId(item, otherCategory)).isEmpty();
    }

    @Test
    void neverSuggestsTheSamePairTwice() {
        UUID report = lose(category("Fone"), "Fone JBL preto", "Bloco 400", "Sala 421", today());
        UUID item = find("Fone preto", "Bloco 400", "Sala 421", today());
        awaitSuggestion(item, report);

        assertThat(matching.matchFoundItem(item)).noneMatch(suggestion -> suggestion.getLossReportId().equals(report));
        assertThat(matching.matchLossReport(report)).noneMatch(suggestion -> suggestion.getFoundItemId().equals(item));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM match_suggestions WHERE found_item_id = ? AND loss_report_id = ?",
                Long.class, item, report)).isOne();
    }

    @Test
    void aFailureInMatchingKeepsTheRegistration() {
        doThrow(new IllegalStateException("falha simulada")).when(matching).matchFoundItem(any());
        UUID report = lose(category("Fone"), "Fone JBL preto", "Bloco 400", "Sala 421", today());

        UUID item = find("Fone preto", "Bloco 400", "Sala 421", today());

        verify(matching, timeout(WAIT.toMillis())).matchFoundItem(item);
        assertThat(items.findById(item)).isPresent();
        assertThat(suggestions.findByFoundItemIdAndLossReportId(item, report)).isEmpty();
    }

    @Test
    void matchesInAVirtualThreadAfterTheCommit() {
        AtomicReference<Thread> thread = new AtomicReference<>();
        AtomicLong savedItems = new AtomicLong(-1);
        doAnswer(invocation -> {
            savedItems.set(jdbc.queryForObject("SELECT count(*) FROM found_items WHERE id = ?", Long.class,
                    invocation.<UUID>getArgument(0)));
            thread.set(Thread.currentThread());
            return invocation.callRealMethod();
        }).when(matching).matchFoundItem(any());

        UUID item = find("Fone preto", "Bloco 400", "Sala 421", today());

        await().atMost(WAIT).untilAsserted(() -> assertThat(thread.get()).isNotNull());
        assertThat(thread.get().isVirtual()).isTrue();
        assertThat(thread.get()).isNotSameAs(Thread.currentThread());
        // O ouvinte já enxerga o objeto numa conexão própria: o cadastro foi confirmado antes do cruzamento.
        assertThat(savedItems.get()).isOne();
        verify(matching).matchFoundItem(item);
    }

    private UUID find(String description, String block, String room, LocalDate foundOn) {
        RegisterFoundItem request = new RegisterFoundItem(TestPhotos.jpeg(), category("Fone"), block, room, foundOn,
                null, null, description, null);
        return foundItems.register(request, UUID.randomUUID()).getId();
    }

    private UUID lose(UUID category, String description, String block, String room, LocalDate day) {
        RegisterLossReport request = new RegisterLossReport(category, description, null,
                List.of(LossLocation.of(block, room)), day, null, null, null);
        return lossReports.register(request, UUID.randomUUID()).getId();
    }

    private MatchSuggestion awaitSuggestion(UUID item, UUID report) {
        return await().atMost(WAIT)
                .until(() -> suggestions.findByFoundItemIdAndLossReportId(item, report), Optional::isPresent)
                .orElseThrow();
    }

    private UUID category(String name) {
        return categories.findByNameIgnoreCase(name).map(Category::getId).orElseThrow();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
