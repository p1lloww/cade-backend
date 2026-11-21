package br.ucsal.cade.pickup.controller.summons;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.status.SuggestionStatus;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Objetos e perdas em categorias só do teste, para o cruzamento automático não criar sugestões a mais. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class SummonsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

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

    private UUID itemCategory;
    private UUID reportCategory;

    @BeforeEach
    void setUp() {
        itemCategory = newCategory();
        reportCategory = newCategory();
    }

    @Test
    void theStaffSummonsTheStudentFromASuggestion() throws Exception {
        UUID item = find();
        MatchSuggestion best = suggest(item, lose(), 80);
        MatchSuggestion other = suggest(item, lose(), 50);

        summon(best.getId(), Role.STAFF)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.suggestionId").value(best.getId().toString()))
                .andExpect(jsonPath("$.foundItemId").value(item.toString()))
                .andExpect(jsonPath("$.lossReportId").value(best.getLossReportId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.statusLabel").value("Ativa"))
                .andExpect(jsonPath("$.expiresAt").value(clock.instant().plus(Duration.ofDays(7)).toString()))
                .andExpect(jsonPath("$.place").value("Sala de achados e perdidos da UCSal"))
                .andExpect(jsonPath("$.hours").value("Das 7h às 21h50"))
                .andExpect(jsonPath("$.whatToBring").isNotEmpty())
                .andExpect(jsonPath("$.onHold", hasSize(1)))
                .andExpect(jsonPath("$.onHold[0]").value(other.getId().toString()))
                .andExpect(jsonPath("$.studentId").doesNotExist());

        assertThat(jdbc.queryForObject("SELECT count(*) FROM audit_records WHERE actor_id = ? AND action = ?",
                Long.class, UUID.fromString(TestTokens.SUBJECT), "SUMMONS_CREATED")).isPositive();
    }

    @Test
    void theAdministratorCanAlsoSummon() throws Exception {
        summon(suggest(find(), lose(), 80).getId(), Role.ADMIN)
                .andExpect(status().isCreated());
    }

    @Test
    void summoningTwiceIsAConflict() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);
        summon(suggestion.getId(), Role.STAFF).andExpect(status().isCreated());

        summon(suggestion.getId(), Role.STAFF)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Não é possível convocar um objeto na situação Reservado."));
    }

    @Test
    void aSuggestionOnHoldCannotBeSummonedWhileTheItemIsReserved() throws Exception {
        UUID item = find();
        MatchSuggestion best = suggest(item, lose(), 80);
        MatchSuggestion other = suggest(item, lose(), 50);
        summon(best.getId(), Role.STAFF).andExpect(status().isCreated());

        summon(other.getId(), Role.STAFF)
                .andExpect(status().isConflict());

        assertThat(suggestions.findById(other.getId())).get().extracting(MatchSuggestion::getStatus)
                .isEqualTo(SuggestionStatus.ON_HOLD);
    }

    @Test
    void anUnknownSuggestionIsNotFound() throws Exception {
        summon(UUID.randomUUID(), Role.STAFF)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sugestão não encontrada."));
    }

    @Test
    void aStudentCannotSummon() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose(), 80);

        summon(suggestion.getId(), Role.STUDENT)
                .andExpect(status().isForbidden());

        assertThat(suggestions.findById(suggestion.getId())).get().extracting(MatchSuggestion::getStatus)
                .isEqualTo(SuggestionStatus.NEW);
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mvc.perform(post("/suggestions/{id}/summon", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions summon(UUID suggestionId, Role role) throws Exception {
        return mvc.perform(post("/suggestions/{id}/summon", suggestionId).header("Authorization", bearer(role)));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private MatchSuggestion suggest(UUID item, UUID report, int score) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, score, clock.instant()));
    }

    private UUID find() {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400", "Sala 421",
                today(), null, null, "Fone preto", null), UUID.randomUUID()).getId();
    }

    private UUID lose() {
        return lossReports.register(new RegisterLossReport(reportCategory, "Fone JBL preto", "Adesivo de estrela",
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
