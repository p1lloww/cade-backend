package br.ucsal.cade.pickup.controller.student;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.pickup.persistence.summons.NewSummons;
import br.ucsal.cade.pickup.persistence.summons.Summons;
import br.ucsal.cade.pickup.persistence.summons.SummonsRepository;
import br.ucsal.cade.shared.category.RiskLevel;
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
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * As convocações são gravadas direto, com ids soltos de objeto, perda e sugestão: a rota só lê a convocação, e as
 * referências a outros módulos não têm chave estrangeira (D10).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class StudentSummonsControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private SummonsRepository summons;

    private UUID student;

    @BeforeEach
    void setUp() {
        student = UUID.randomUUID();
    }

    @Test
    void theStudentSeesWhereUntilWhenAndWhatToBringForEachActiveSummons() throws Exception {
        Summons later = issue(student, RiskLevel.LOW, Duration.ofDays(7));
        Summons sooner = issue(student, RiskLevel.HIGH, Duration.ofDays(2));

        mine()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(sooner.getId().toString()))
                .andExpect(jsonPath("$[0].lossReportId").value(sooner.getLossReportId().toString()))
                .andExpect(jsonPath("$[0].summonedAt").value(sooner.getSummonedAt().toString()))
                .andExpect(jsonPath("$[0].expiresAt").value(sooner.getExpiresAt().toString()))
                .andExpect(jsonPath("$[0].place").value("Sala de achados e perdidos da UCSal"))
                .andExpect(jsonPath("$[0].hours").value("Das 7h às 21h50"))
                .andExpect(jsonPath("$[0].whatToBring").isNotEmpty())
                .andExpect(jsonPath("$[1].id").value(later.getId().toString()));
    }

    @Test
    void whatToBringFollowsTheRiskOfTheItem() throws Exception {
        issue(student, RiskLevel.LOW, Duration.ofDays(2));
        issue(student, RiskLevel.HIGH, Duration.ofDays(3));

        String body = mine().andReturn().getResponse().getContentAsString();

        String low = JsonPath.read(body, "$[0].whatToBring");
        String high = JsonPath.read(body, "$[1].whatToBring");
        assertThat(high).isNotEqualTo(low);
    }

    @Test
    void summonsThatAreOverOrOfOtherStudentsAreLeftOut() throws Exception {
        Summons active = issue(student, RiskLevel.LOW, Duration.ofDays(7));
        Summons completed = issue(student, RiskLevel.LOW, Duration.ofDays(7));
        completed.complete(UUID.randomUUID(), clock.instant());
        summons.saveAndFlush(completed);
        Summons refused = issue(student, RiskLevel.LOW, Duration.ofDays(7));
        refused.refuse(UUID.randomUUID(), clock.instant(), "Não soube dizer o detalhe.");
        summons.saveAndFlush(refused);
        issue(UUID.randomUUID(), RiskLevel.LOW, Duration.ofDays(7));

        mine()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(active.getId().toString()));
    }

    @Test
    void aStudentWithoutSummonsGetsAnEmptyList() throws Exception {
        mine()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void bringsNothingAboutTheFoundItem() throws Exception {
        issue(student, RiskLevel.MEDIUM, Duration.ofDays(7));

        String body = mine().andReturn().getResponse().getContentAsString();

        Map<String, Object> summons = JsonPath.read(body, "$[0]");
        assertThat(summons.keySet()).containsExactlyInAnyOrder("id", "lossReportId", "summonedAt", "expiresAt",
                "place", "hours", "whatToBring");
    }

    @Test
    void onlyStudentsListTheirSummons() throws Exception {
        for (Role role : new Role[]{Role.STAFF, Role.ADMIN}) {
            mvc.perform(get("/summons/mine").header("Authorization",
                            "Bearer " + TestTokens.forRole(encoder, clock.instant(), role)))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/summons/mine")).andExpect(status().isUnauthorized());
    }

    private Summons issue(UUID studentId, RiskLevel risk, Duration deadline) {
        Instant now = clock.instant();
        return summons.saveAndFlush(Summons.issue(new NewSummons(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), studentId, UUID.randomUUID(), risk), now, now.plus(deadline)));
    }

    private ResultActions mine() throws Exception {
        String token = TestTokens.token(encoder, clock.instant(), claims -> claims.subject(student.toString())
                .claim(TokenClaims.ROLE, Role.STUDENT.name()));
        return mvc.perform(get("/summons/mine").header("Authorization", "Bearer " + token));
    }
}
