package br.ucsal.cade.matching.controller.suggestion;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Objetos e perdas em categorias só do teste, para o cruzamento automático não criar sugestões a mais. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class SuggestionControllerTest {

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
    void theStaffSeesTheSuggestionsOfAnItemSideBySide() throws Exception {
        UUID item = find();
        UUID high = lose("Fone JBL preto");
        UUID medium = lose("Fone branco");
        suggest(item, medium, 45);
        MatchSuggestion best = suggest(item, high, 80);

        mvc.perform(get("/found-items/{id}/suggestions", item).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(best.getId().toString()))
                .andExpect(jsonPath("$[0].score").value(80))
                .andExpect(jsonPath("$[0].band").value("HIGH"))
                .andExpect(jsonPath("$[0].bandLabel").value("Alta"))
                .andExpect(jsonPath("$[0].status").value("NEW"))
                .andExpect(jsonPath("$[0].statusLabel").value("Nova"))
                .andExpect(jsonPath("$[0].foundItem.id").value(item.toString()))
                .andExpect(jsonPath("$[0].foundItem.internalDescription").value("Fone preto"))
                .andExpect(jsonPath("$[0].foundItem.reservedDetails").value("Arranhão na haste"))
                .andExpect(jsonPath("$[0].foundItem.photoPath").value("/found-items/" + item + "/photo"))
                .andExpect(jsonPath("$[0].lossReport.id").value(high.toString()))
                .andExpect(jsonPath("$[0].lossReport.description").value("Fone JBL preto"))
                .andExpect(jsonPath("$[0].lossReport.secretDetail").value("Adesivo de estrela"))
                .andExpect(jsonPath("$[0].lossReport.locations[0].room").value("Sala 421"))
                .andExpect(jsonPath("$[0].lossReport.studentId").doesNotExist())
                .andExpect(jsonPath("$[1].band").value("MEDIUM"));
    }

    @Test
    void theStaffSeesTheSuggestionsOfAReport() throws Exception {
        UUID report = lose("Fone JBL preto");
        UUID item = find();
        suggest(item, report, 80);

        mvc.perform(get("/loss-reports/{id}/suggestions", report).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].foundItem.id").value(item.toString()));
    }

    @Test
    void theAdministratorAlsoSeesTheSuggestions() throws Exception {
        UUID item = find();
        suggest(item, lose("Fone JBL preto"), 80);

        mvc.perform(get("/found-items/{id}/suggestions", item).header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void findsOneSuggestion() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);

        mvc.perform(get("/suggestions/{id}", suggestion.getId()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suggestion.getId().toString()))
                .andExpect(jsonPath("$.lossReport.id").value(suggestion.getLossReportId().toString()));
    }

    @Test
    void anUnknownSuggestionIsNotFound() throws Exception {
        mvc.perform(get("/suggestions/{id}", UUID.randomUUID()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Sugestão não encontrada."));
        discard(UUID.randomUUID(), "Motivo.")
                .andExpect(status().isNotFound());
    }

    @Test
    void theStaffDiscardsASuggestionWithAReason() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);

        discard(suggestion.getId(), "A cor não bate com a foto.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCARDED"))
                .andExpect(jsonPath("$.statusLabel").value("Descartada"));

        assertThat(suggestions.findById(suggestion.getId())).get().extracting(MatchSuggestion::getStatus)
                .isEqualTo(SuggestionStatus.DISCARDED);
        assertThat(jdbc.queryForObject("SELECT reason FROM audit_records WHERE entity_id = ? AND actor_id = ?",
                String.class, suggestion.getId(), UUID.fromString(TestTokens.SUBJECT)))
                .isEqualTo("A cor não bate com a foto.");
    }

    @Test
    void discardingWithoutAReasonIsABadRequest() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);

        discard(suggestion.getId(), " ")
                .andExpect(status().isBadRequest());

        assertThat(suggestions.findById(suggestion.getId())).get().extracting(MatchSuggestion::getStatus)
                .isEqualTo(SuggestionStatus.NEW);
    }

    @Test
    void discardingTwiceIsAConflict() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);
        discard(suggestion.getId(), "Primeiro.").andExpect(status().isOk());

        discard(suggestion.getId(), "Segundo.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Não é possível descartar uma sugestão na situação Descartada."));
    }

    @Test
    void aStudentCannotSeeOrDiscardSuggestions() throws Exception {
        MatchSuggestion suggestion = suggest(find(), lose("Fone JBL preto"), 80);

        mvc.perform(get("/found-items/{id}/suggestions", suggestion.getFoundItemId())
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/loss-reports/{id}/suggestions", suggestion.getLossReportId())
                        .header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/suggestions/{id}/discard", suggestion.getId())
                        .header("Authorization", bearer(Role.STUDENT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"Motivo.\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mvc.perform(get("/suggestions/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions discard(UUID id, String reason) throws Exception {
        return mvc.perform(post("/suggestions/{id}/discard", id)
                .header("Authorization", bearer(Role.STAFF))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"" + reason + "\"}"));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private MatchSuggestion suggest(UUID item, UUID report, int score) {
        return suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, score, clock.instant()));
    }

    private UUID find() {
        return foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400", "Sala 421",
                today(), null, null, "Fone preto", "Arranhão na haste"), UUID.randomUUID()).getId();
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
