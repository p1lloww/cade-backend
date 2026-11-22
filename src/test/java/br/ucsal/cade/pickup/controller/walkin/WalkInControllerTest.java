package br.ucsal.cade.pickup.controller.walkin;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class WalkInControllerTest {

    private static final String STUDENT = """
            {"foundItemId": "%s", "holderType": "STUDENT", "holderName": " Estudante de Teste ",
             "documentType": "STUDENT_CARD", "documentNumber": "000000", "enrollment": "%s",
             "verification": "%s"}""";

    private static final String VISITOR = """
            {"foundItemId": "%s", "holderType": "VISITOR", "holderName": "Pessoa Visitante",
             "documentType": "RG", "documentNumber": "000000", "verification": "DESCRIPTION_MATCHED"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private FoundItemService foundItemService;

    @Autowired
    private FoundItemRepository foundItems;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theStaffSeesWhatToCheckForTheRiskOfTheItem() throws Exception {
        UUID item = find(RiskLevel.HIGH);

        mvc.perform(get("/found-items/{id}/walk-in-check", item).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.foundItemId").value(item.toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.statusLabel").value("Disponível"))
                .andExpect(jsonPath("$.canHandOver").value(true))
                .andExpect(jsonPath("$.risk").value("HIGH"))
                .andExpect(jsonPath("$.riskLabel").value("Alto"))
                .andExpect(jsonPath("$.checklist[0]").value(containsString("Documento com foto")))
                .andExpect(jsonPath("$.acceptedVerifications[*].code").value(containsInAnyOrder("DEVICE_UNLOCKED",
                        "PROOF_OF_OWNERSHIP")))
                .andExpect(jsonPath("$.internalDescription").doesNotExist());
    }

    @Test
    void aReservedItemShowsThatItCannotLeaveThroughTheShortcut() throws Exception {
        UUID item = find(RiskLevel.LOW);
        jdbc.update("UPDATE found_items SET status = 'RESERVED' WHERE id = ?", item);

        mvc.perform(get("/found-items/{id}/walk-in-check", item).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canHandOver").value(false));
        register(STUDENT.formatted(item, "202600001", "DESCRIPTION_MATCHED"), Role.STAFF)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail")
                        .value("Não é possível entregar sem convocação um objeto na situação Reservado."));
    }

    @Test
    void theStaffRegistersTheShortcutForAStudent() throws Exception {
        UUID item = find(RiskLevel.LOW);
        MatchSuggestion suggestion = suggestions.saveAndFlush(MatchSuggestion.suggest(item, UUID.randomUUID(), 60,
                clock.instant()));

        register(STUDENT.formatted(item, "202600001", "DESCRIPTION_MATCHED"), Role.STAFF)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.foundItemId").value(item.toString()))
                .andExpect(jsonPath("$.holderType").value("STUDENT"))
                .andExpect(jsonPath("$.holderTypeLabel").value("Estudante"))
                .andExpect(jsonPath("$.holderName").value("Estudante de Teste"))
                .andExpect(jsonPath("$.documentType").value("Carteirinha da UCSal"))
                .andExpect(jsonPath("$.verification").value("Descrição coerente com o objeto"))
                .andExpect(jsonPath("$.handedOverBy").value(TestTokens.SUBJECT))
                .andExpect(jsonPath("$.closedSuggestions", hasSize(1)))
                .andExpect(jsonPath("$.closedSuggestions[0]").value(suggestion.getId().toString()));

        assertThat(foundItems.findById(item).orElseThrow().getStatus()).isEqualTo(FoundItemStatus.RETURNED);
    }

    @Test
    void theAdministratorRegistersTheShortcutForAPersonWithoutAccount() throws Exception {
        UUID item = find(RiskLevel.LOW);

        register(VISITOR.formatted(item), Role.ADMIN)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holderType").value("VISITOR"))
                .andExpect(jsonPath("$.holderTypeLabel").value("Pessoa sem conta"));
    }

    @Test
    void aStudentWithoutEnrollmentAndAWeakVerificationAreRefused() throws Exception {
        UUID item = find(RiskLevel.HIGH);

        register(STUDENT.formatted(item, "", "DEVICE_UNLOCKED"), Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Informe a matrícula")));
        register(STUDENT.formatted(item, "202600001", "DESCRIPTION_MATCHED"), Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("não basta para um objeto de risco Alto")));
        assertThat(foundItems.findById(item).orElseThrow().getStatus()).isEqualTo(FoundItemStatus.AVAILABLE);
    }

    @Test
    void asksForEachRequiredField() throws Exception {
        register("{}", Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.foundItemId").value("Informe o objeto."))
                .andExpect(jsonPath("$.errors.holderType")
                        .value("Informe se quem retira é estudante ou pessoa sem conta."))
                .andExpect(jsonPath("$.errors.holderName").value("Informe o nome de quem retira."))
                .andExpect(jsonPath("$.errors.documentType").value("Informe o tipo do documento com foto."))
                .andExpect(jsonPath("$.errors.documentNumber").value("Informe o número do documento."))
                .andExpect(jsonPath("$.errors.verification").value("Informe a verificação feita."));
    }

    @Test
    void anUnknownItemIsNotFound() throws Exception {
        mvc.perform(get("/found-items/{id}/walk-in-check", UUID.randomUUID())
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound());
        register(VISITOR.formatted(UUID.randomUUID()), Role.STAFF)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Objeto não encontrado."));
    }

    @Test
    void onlyStaffAndAdministratorsUseTheShortcut() throws Exception {
        UUID item = find(RiskLevel.LOW);

        mvc.perform(get("/found-items/{id}/walk-in-check", item).header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        register(VISITOR.formatted(item), Role.STUDENT)
                .andExpect(status().isForbidden());
        mvc.perform(get("/found-items/{id}/walk-in-check", item))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/walk-in-pickups").contentType(MediaType.APPLICATION_JSON).content(VISITOR.formatted(item)))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions register(String json, Role role) throws Exception {
        return mvc.perform(post("/walk-in-pickups")
                .header("Authorization", bearer(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    /** Numa categoria só deste teste, sem perdas, para o cruzamento não criar sugestões a mais. */
    private UUID find(RiskLevel risk) {
        UUID category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), risk)).getId();
        return foundItemService.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 300", "Sala 305",
                LocalDate.now(clock), null, null, "Garrafa azul", null), UUID.randomUUID()).getId();
    }
}
