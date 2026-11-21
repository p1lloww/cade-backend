package br.ucsal.cade.pickup.controller.counter;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestion;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.pickup.service.summons.SummonsService;
import br.ucsal.cade.pickup.service.verification.VerificationRule;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Objetos e perdas em categorias só do teste, para o cruzamento automático não criar sugestões a mais. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class CounterControllerTest {

    private static final String PICKUP = """
            {"documentType": "STUDENT_CARD", "documentNumber": "000000", "enrollment": "000000000",
             "verification": "%s", "notes": "  "}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private SummonsService summonsService;

    @Autowired
    private SummonsRepository summonses;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private FoundItemService foundItems;

    @Autowired
    private LossReportService lossReports;

    @Autowired
    private CategoryRepository categories;

    private UUID reportCategory;

    @BeforeEach
    void setUp() {
        reportCategory = newCategory(RiskLevel.MEDIUM);
    }

    @Test
    void theStaffSeesWhatToCheckForTheRisk() throws Exception {
        Summons summons = summon(RiskLevel.HIGH);

        mvc.perform(get("/summons/{id}", summons.getId()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(summons.getId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.risk").value("HIGH"))
                .andExpect(jsonPath("$.riskLabel").value("Alto"))
                .andExpect(jsonPath("$.checklist[0]").value(VerificationRule.PHOTO_ID_CHECK))
                .andExpect(jsonPath("$.acceptedVerifications", hasSize(2)))
                .andExpect(jsonPath("$.acceptedVerifications[*].code", hasItem("DEVICE_UNLOCKED")))
                .andExpect(jsonPath("$.acceptedVerifications[*].code", hasItem("PROOF_OF_OWNERSHIP")))
                .andExpect(jsonPath("$.studentId").doesNotExist());
    }

    @Test
    void theActiveListHasTheSummons() throws Exception {
        Summons summons = summon(RiskLevel.MEDIUM);

        mvc.perform(get("/summons").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(summons.getId().toString())));
    }

    @Test
    void anUnknownSummonsIsNotFound() throws Exception {
        mvc.perform(get("/summons/{id}", UUID.randomUUID()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Convocação não encontrada."));
    }

    @Test
    void theStaffRegistersThePickup() throws Exception {
        Summons summons = summon(RiskLevel.MEDIUM);

        pickup(summons.getId(), "DETAIL_CONFIRMED", Role.STAFF)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.summonsId").value(summons.getId().toString()))
                .andExpect(jsonPath("$.foundItemId").value(summons.getFoundItemId().toString()))
                .andExpect(jsonPath("$.documentType").value("Carteirinha da UCSal"))
                .andExpect(jsonPath("$.handedOverBy").value(TestTokens.SUBJECT))
                .andExpect(jsonPath("$.handedOverAt").value(clock.instant().toString()));

        assertThat(statusOf(summons)).isEqualTo(SummonsStatus.COMPLETED);
    }

    @Test
    void theAdministratorCanAlsoRegisterThePickup() throws Exception {
        pickup(summon(RiskLevel.MEDIUM).getId(), "DETAIL_CONFIRMED", Role.ADMIN)
                .andExpect(status().isCreated());
    }

    @Test
    void aVerificationThatIsNotEnoughForTheRiskIsABadRequest() throws Exception {
        Summons summons = summon(RiskLevel.HIGH);

        pickup(summons.getId(), "DESCRIPTION_MATCHED", Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", startsWith("Essa verificação não basta para um objeto de risco Alto.")));

        assertThat(statusOf(summons)).isEqualTo(SummonsStatus.ACTIVE);
    }

    @Test
    void aPickupWithoutTheDocumentIsABadRequest() throws Exception {
        mvc.perform(post("/summons/{id}/pickup", summon(RiskLevel.MEDIUM).getId())
                        .header("Authorization", bearer(Role.STAFF))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"enrollment": "000000000", "verification": "DETAIL_CONFIRMED"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theStaffRegistersTheRefusal() throws Exception {
        Summons summons = summon(RiskLevel.MEDIUM);

        refuse(summons.getId(), "Não soube descrever o detalhe.", Role.STAFF)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summonsId").value(summons.getId().toString()))
                .andExpect(jsonPath("$.status").value("REFUSED"))
                .andExpect(jsonPath("$.statusLabel").value("Recusada"))
                .andExpect(jsonPath("$.reason").value("Não soube descrever o detalhe."))
                .andExpect(jsonPath("$.studentRefusals").value(1))
                .andExpect(jsonPath("$.studentBlocked").value(false))
                .andExpect(jsonPath("$.resumed", hasSize(0)))
                .andExpect(jsonPath("$.closed", hasSize(0)));
    }

    @Test
    void aRefusalWithoutAReasonIsABadRequest() throws Exception {
        Summons summons = summon(RiskLevel.MEDIUM);

        refuse(summons.getId(), " ", Role.STAFF)
                .andExpect(status().isBadRequest());

        assertThat(statusOf(summons)).isEqualTo(SummonsStatus.ACTIVE);
    }

    @Test
    void aPickupAfterTheRefusalIsAConflict() throws Exception {
        Summons summons = summon(RiskLevel.MEDIUM);
        refuse(summons.getId(), "Verificação falhou.", Role.STAFF).andExpect(status().isOk());

        pickup(summons.getId(), "DETAIL_CONFIRMED", Role.STAFF)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Não é possível concluir uma convocação na situação Recusada."));
    }

    @Test
    void aStudentCannotUseTheCounter() throws Exception {
        Summons summons = summon(RiskLevel.MEDIUM);

        mvc.perform(get("/summons/{id}", summons.getId()).header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        pickup(summons.getId(), "DETAIL_CONFIRMED", Role.STUDENT)
                .andExpect(status().isForbidden());
        refuse(summons.getId(), "Verificação falhou.", Role.STUDENT)
                .andExpect(status().isForbidden());

        assertThat(statusOf(summons)).isEqualTo(SummonsStatus.ACTIVE);
    }

    @Test
    void withoutATokenIsUnauthorized() throws Exception {
        mvc.perform(get("/summons"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions pickup(UUID summonsId, String verification, Role role) throws Exception {
        return mvc.perform(post("/summons/{id}/pickup", summonsId)
                .header("Authorization", bearer(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PICKUP.formatted(verification)));
    }

    private ResultActions refuse(UUID summonsId, String reason, Role role) throws Exception {
        return mvc.perform(post("/summons/{id}/refusal", summonsId)
                .header("Authorization", bearer(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\": \"" + reason + "\"}"));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private SummonsStatus statusOf(Summons summons) {
        return summonses.findById(summons.getId()).orElseThrow().getStatus();
    }

    private Summons summon(RiskLevel risk) {
        UUID item = foundItems.register(new RegisterFoundItem(TestPhotos.jpeg(), newCategory(risk), "Bloco 400",
                "Sala 421", today(), null, null, "Fone preto", null), UUID.randomUUID()).getId();
        UUID report = lossReports.register(new RegisterLossReport(reportCategory, "Fone JBL preto",
                "Adesivo de estrela", List.of(LossLocation.of("Bloco 400", "Sala 421")), today(), null, null, null),
                UUID.randomUUID()).getId();
        MatchSuggestion suggestion = suggestions.saveAndFlush(MatchSuggestion.suggest(item, report, 80,
                clock.instant()));
        return summonsService.summon(suggestion.getId(), UUID.randomUUID()).summons();
    }

    private UUID newCategory(RiskLevel risk) {
        return categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), risk)).getId();
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
