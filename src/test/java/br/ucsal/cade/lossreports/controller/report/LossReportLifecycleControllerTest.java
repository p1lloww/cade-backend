package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.mail.EmailSender;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** As rotas do cancelamento, da resposta ao pedido de informações, da renovação (estudante) e do pedido (funcionário). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class LossReportLifecycleControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private LossReportService lossReportService;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private EmailSender emailSender;

    private UUID student;
    private UUID category;

    @BeforeEach
    void setUp() {
        when(emailSender.send(any())).thenReturn(true);
        student = UUID.randomUUID();
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.LOW)).getId();
    }

    @Test
    void theStudentCancelsHisOpenReport() throws Exception {
        UUID report = lose();

        mvc.perform(post("/loss-reports/{id}/cancel", report).header("Authorization", studentBearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.statusLabel").value("Cancelado"));

        mvc.perform(post("/loss-reports/{id}/cancel", report).header("Authorization", studentBearer(student)))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherStudentGetsNotFoundOnEveryLifecycleRoute() throws Exception {
        UUID report = lose();
        String other = studentBearer(UUID.randomUUID());

        mvc.perform(post("/loss-reports/{id}/cancel", report).header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(post("/loss-reports/{id}/renew", report).header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(post("/loss-reports/{id}/info-response", report).header("Authorization", other)
                .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"Azul\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffAsksForInfoThenTheStudentAnswersAndSeesTheQuestionAndHisAnswer() throws Exception {
        UUID report = lose();
        MatchSuggestion suggestion = suggestions.saveAndFlush(
                MatchSuggestion.suggest(find(), report, 70, clock.instant()));

        mvc.perform(post("/suggestions/{id}/info-request", suggestion.getId())
                        .header("Authorization", bearer(Role.STAFF)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"A capa tem alguma marca?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEW"));

        mvc.perform(post("/loss-reports/{id}/info-response", report).header("Authorization", studentBearer(student))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"answer\":\"  Um risco na tampa  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.infoRequestReason").value("A capa tem alguma marca?"))
                .andExpect(jsonPath("$.infoAnswer").value("Um risco na tampa"));
    }

    private UUID find() {
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                LocalDate.now(clock), null, null, "Garrafa azul", null), UUID.randomUUID()).getId();
    }

    private UUID lose() {
        return lossReportService.register(new RegisterLossReport(category, "Garrafa azul", null,
                List.of(LossLocation.of("Bloco 400", "Sala 421")), LocalDate.now(clock), null, null, null),
                student).getId();
    }

    private String studentBearer(UUID id) {
        return "Bearer " + TestTokens.token(encoder, clock.instant(), claims -> claims.subject(id.toString())
                .claim(TokenClaims.ROLE, Role.STUDENT.name()));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }
}
