package br.ucsal.cade.pickup.controller.student;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import br.ucsal.cade.pickup.persistence.pickup.IdDocumentType;
import br.ucsal.cade.pickup.persistence.pickup.Pickup;
import br.ucsal.cade.pickup.persistence.pickup.VerificationMethod;
import br.ucsal.cade.pickup.service.counter.CounterService;
import br.ucsal.cade.pickup.service.summons.SummonsService;
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
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** As rotas da confirmação da retirada pelo estudante (RF30). */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class StudentPickupControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CounterService counter;

    @Autowired
    private SummonsService summonsService;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private LossReportService lossReportService;

    @Autowired
    private CategoryRepository categories;

    @MockitoBean
    private EmailSender emailSender;

    private UUID student;
    private UUID itemCategory;
    private UUID reportCategory;

    @BeforeEach
    void setUp() {
        when(emailSender.send(any())).thenReturn(true);
        student = UUID.randomUUID();
        // Categorias diferentes para o objeto e a perda: o cruzamento automático, que roda depois do commit, dá nota
        // zero e não grava a sugestão que o teste grava (o par é único).
        itemCategory = newCategory();
        reportCategory = newCategory();
    }

    @Test
    void theStudentListsHisPickupsAndConfirmsOneWithoutSeeingAnythingOfTheObject() throws Exception {
        Pickup pickup = pickupOf(student);

        String body = mvc.perform(get("/pickups/mine").header("Authorization", studentBearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pickupId").value(pickup.getId().toString()))
                .andExpect(jsonPath("$[0].confirmed").value(false))
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> first = JsonPath.read(body, "$[0]");
        assertThat(first.keySet()).containsExactlyInAnyOrder("pickupId", "handedOverAt", "confirmed", "confirmedAt");

        mvc.perform(post("/pickups/{id}/confirmation", pickup.getId()).header("Authorization", studentBearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true))
                .andExpect(jsonPath("$.confirmedAt").value(clock.instant().toString()));
        mvc.perform(get("/pickups/mine").header("Authorization", studentBearer(student)))
                .andExpect(jsonPath("$[0].confirmed").value(true));
    }

    @Test
    void confirmingTwiceIsAConflictAndAnotherStudentGetsNotFound() throws Exception {
        Pickup pickup = pickupOf(student);
        mvc.perform(post("/pickups/{id}/confirmation", pickup.getId()).header("Authorization", studentBearer(student)))
                .andExpect(status().isOk());

        mvc.perform(post("/pickups/{id}/confirmation", pickup.getId()).header("Authorization", studentBearer(student)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Você já confirmou esta retirada."));
        mvc.perform(post("/pickups/{id}/confirmation", pickup.getId())
                        .header("Authorization", studentBearer(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    private Pickup pickupOf(UUID owner) {
        UUID item = foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), itemCategory, "Bloco 400",
                "Sala 421", LocalDate.now(clock), null, null, "Fone preto", null), UUID.randomUUID()).getId();
        UUID report = lossReportService.register(new RegisterLossReport(reportCategory, "Fone JBL preto", "Adesivo",
                List.of(LossLocation.of("Bloco 400", "Sala 421")), LocalDate.now(clock), null, null, null),
                owner).getId();
        MatchSuggestion suggestion = suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, 80,
                clock.instant()));
        UUID summons = summonsService.summon(suggestion.getId(), UUID.randomUUID()).summons().getId();
        return counter.registerPickup(summons, new CounterCheck(IdDocumentType.STUDENT_CARD, "000000", "000000000",
                VerificationMethod.DETAIL_CONFIRMED, null), UUID.randomUUID());
    }

    private UUID newCategory() {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
    }

    private String studentBearer(UUID id) {
        return "Bearer " + TestTokens.token(encoder, clock.instant(), claims -> claims.subject(id.toString())
                .claim(TokenClaims.ROLE, Role.STUDENT.name()));
    }

}
