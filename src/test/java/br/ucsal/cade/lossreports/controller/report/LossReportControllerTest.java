package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.lossreports.api.event.LossReportRegisteredEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class LossReportControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private AuditRecordRepository auditRecords;

    @Autowired
    private ApplicationEvents events;

    /** Cada teste com um estudante novo, para o limite de 5 (RN05) de um não atrapalhar o outro. */
    private UUID student;

    @BeforeEach
    void setUp() {
        student = UUID.randomUUID();
    }

    @Test
    void theStudentRegistersALossAndSeesTheSecretDetail() throws Exception {
        String body = register("""
                {"categoryId": "%s", "description": " Fone JBL preto ", "secretDetail": "Adesivo de estrela",
                 "locations": [{"block": "Bloco 400", "room": "Sala 421"}, {"block": "Bloco B"}],
                 "lostFrom": "%s", "shift": "EVENING"}""".formatted(headphones(), yesterday()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/loss-reports/")))
                .andExpect(jsonPath("$.categoryName").value("Fone"))
                .andExpect(jsonPath("$.description").value("Fone JBL preto"))
                .andExpect(jsonPath("$.secretDetail").value("Adesivo de estrela"))
                .andExpect(jsonPath("$.locations[0].label").value("Bloco 400, Sala 421"))
                .andExpect(jsonPath("$.locations[1].room").doesNotExist())
                .andExpect(jsonPath("$.locations[1].label").value("Bloco B, sala não informada"))
                .andExpect(jsonPath("$.locationsLabel").value("Bloco 400, Sala 421; Bloco B, sala não informada"))
                .andExpect(jsonPath("$.lostFrom").value(yesterday().toString()))
                .andExpect(jsonPath("$.lostUntil").value(yesterday().toString()))
                .andExpect(jsonPath("$.shiftLabel").value("Noite"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.statusLabel").value("Aberto"))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(body, "$.id"));
        assertThat(events.stream(LossReportRegisteredEvent.class)).containsExactly(
                new LossReportRegisteredEvent(id, headphones(), yesterday(), yesterday()));
        assertThat(auditRecords.findAll()).filteredOn(record -> record.getEntityId().equals(id))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getAction()).isEqualTo("LOSS_REPORT_REGISTERED");
                    assertThat(record.getActorId()).isEqualTo(student);
                });
    }

    @Test
    void whatWasNotInformedComesAsNotInformed() throws Exception {
        register(minimal())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locations").isEmpty())
                .andExpect(jsonPath("$.locationsLabel").value("Local não informado"))
                .andExpect(jsonPath("$.shift").doesNotExist())
                .andExpect(jsonPath("$.shiftLabel").value("Turno não informado"))
                .andExpect(jsonPath("$.secretDetail").doesNotExist());
    }

    @Test
    void onlyStudentsRegisterLosses() throws Exception {
        for (Role role : new Role[]{Role.STAFF, Role.ADMIN}) {
            mvc.perform(post("/loss-reports").header("Authorization", bearer(role))
                            .contentType(MediaType.APPLICATION_JSON).content(minimal()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/loss-reports").contentType(MediaType.APPLICATION_JSON).content(minimal()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void asksForEachRequiredField() throws Exception {
        register("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categoryId").value("Escolha a categoria."))
                .andExpect(jsonPath("$.errors.description").value("Descreva o objeto."))
                .andExpect(jsonPath("$.errors.lostFrom").value("Informe quando perdeu o objeto."));
    }

    @Test
    void validatesEachLocation() throws Exception {
        register("""
                {"categoryId": "%s", "description": "Fone", "lostFrom": "%s",
                 "locations": [{"block": "Bloco 400"}, {"block": " ", "room": "Sala 421"}]}"""
                .formatted(headphones(), yesterday()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['locations[1].block']").value("Informe o bloco de cada local."));
    }

    @Test
    void refusesMoreThanFiveLocations() throws Exception {
        String locations = String.join(",", IntStream.rangeClosed(1, 6)
                .mapToObj(n -> "{\"block\": \"Bloco %d\"}".formatted(n)).toList());

        register("""
                {"categoryId": "%s", "description": "Fone", "lostFrom": "%s", "locations": [%s]}"""
                .formatted(headphones(), yesterday(), locations))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.locations").value("Informe no máximo 5 locais."));
    }

    @Test
    void refusesAFutureDateAndAnInvertedPeriod() throws Exception {
        register("""
                {"categoryId": "%s", "description": "Fone", "lostFrom": "%s"}"""
                .formatted(headphones(), yesterday().plusDays(2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data da perda não pode ser no futuro."));
        register("""
                {"categoryId": "%s", "description": "Fone", "lostFrom": "%s", "lostUntil": "%s"}"""
                .formatted(headphones(), yesterday(), yesterday().minusDays(3)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O início do período não pode ser depois do fim."));
    }

    @Test
    void refusesAnUnknownCategoryAndAnUnknownShift() throws Exception {
        register("""
                {"categoryId": "%s", "description": "Fone", "lostFrom": "%s"}"""
                .formatted(UUID.randomUUID(), yesterday()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Categoria não encontrada."));
        register("""
                {"categoryId": "%s", "description": "Fone", "lostFrom": "%s", "shift": "MADRUGADA"}"""
                .formatted(headphones(), yesterday()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theSixthReportInProgressIsRefusedWith409() throws Exception {
        for (int i = 0; i < 5; i++) {
            register(minimal()).andExpect(status().isCreated());
        }

        register(minimal())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(startsWith("Você já tem 5 registros em andamento.")));
    }

    private ResultActions register(String json) throws Exception {
        return mvc.perform(post("/loss-reports").header("Authorization", "Bearer " + studentToken())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String minimal() {
        return """
                {"categoryId": "%s", "description": "Garrafa azul", "lostFrom": "%s"}"""
                .formatted(headphones(), yesterday());
    }

    private String studentToken() {
        return TestTokens.token(encoder, clock.instant(), claims -> claims.subject(student.toString())
                .claim(TokenClaims.ROLE, Role.STUDENT.name()));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private UUID headphones() {
        return categories.findByNameIgnoreCase("Fone").map(Category::getId).orElseThrow();
    }

    private LocalDate yesterday() {
        return LocalDate.now(clock).minusDays(1);
    }
}
