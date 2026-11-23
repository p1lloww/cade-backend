package br.ucsal.cade.demo.service;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.persistence.item.FoundItem;
import br.ucsal.cade.founditems.persistence.item.FoundItemRepository;
import br.ucsal.cade.lossreports.persistence.report.LossReport;
import br.ucsal.cade.lossreports.persistence.report.LossReportRepository;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O modo demonstração (RF47) de ponta a ponta, com o profile {@code demo}: as três contas, o login direto, os dados de
 * exemplo e o caso do registro genérico ("iPhone"), que recebe nota baixa no cruzamento (critério de sucesso 4). O
 * contexto tem o próprio banco, então os dados de exemplo não vazam para os outros testes.
 */
@SpringBootTest
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class DemoModeTest {

    /** Abaixo da faixa alta da compatibilidade (`cade.matching.bands.high`). */
    private static final int HIGH_BAND = 70;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder decoder;

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private FoundItemRepository foundItems;

    @Autowired
    private LossReportRepository lossReports;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void listsOneFictitiousAccountPerRole() throws Exception {
        mvc.perform(get("/demo/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].email").value("estudante.demo@ucsal.edu.br"))
                .andExpect(jsonPath("$[1].role").value("STAFF"))
                .andExpect(jsonPath("$[2].role").value("ADMIN"));
    }

    @Test
    void logsInDirectlyWithoutACodeAndTheTokenCarriesTheRole() throws Exception {
        for (Role role : Role.values()) {
            String token = login(role);

            assertThat(decoder.decode(token).<String>getClaim(TokenClaims.ROLE)).isEqualTo(role.name());
            assertThat(decoder.decode(token).<String>getClaim(TokenClaims.EMAIL)).endsWith("@ucsal.edu.br");
        }
    }

    @Test
    void createsTheSampleItemsAndTheStudentsReports() throws Exception {
        assertThat(foundItems.findAll()).hasSize(4);

        mvc.perform(get("/loss-reports/mine").header("Authorization", "Bearer " + login(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.description == 'iPhone')]", hasSize(1)));
    }

    @Test
    void startingAgainDoesNotDuplicateTheSampleData() throws Exception {
        seeder.run(null);
        seeder.run(null);

        assertThat(foundItems.findAll()).hasSize(4);
        assertThat(lossReports.findAll()).hasSize(3);
    }

    @Test
    void theGenericIphoneReportScoresLowAndTheDetailedReportScoresHigh() {
        LossReport generic = reportDescribedAs("iPhone");
        LossReport detailed = reportDescribedAs("Fone de ouvido JBL preto");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(scoresOf(generic)).isNotEmpty();
            assertThat(scoresOf(detailed)).isNotEmpty();
        });

        // Categoria igual e data (40) + uma palavra em comum (10): na faixa média, nunca na alta (RN07).
        assertThat(scoresOf(generic)).allSatisfy(score -> assertThat(score).isLessThanOrEqualTo(50).isLessThan(HIGH_BAND));
        assertThat(scoresOf(detailed)).anySatisfy(score -> assertThat(score).isGreaterThanOrEqualTo(HIGH_BAND));
    }

    @Test
    void theSampleFoundItemsKeepTheirReservedDetailsOutOfTheStudentsView() throws Exception {
        FoundItem phone = foundItems.findAll().stream()
                .filter(item -> "iPhone com capa transparente".equals(item.getInternalDescription()))
                .findFirst().orElseThrow();

        String body = mvc.perform(get("/loss-reports/mine").header("Authorization", "Bearer " + login(Role.STUDENT)))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(phone.getId().toString()).doesNotContain("capa transparente")
                .doesNotContain("papel de parede");
    }

    private String login(Role role) throws Exception {
        String body = mvc.perform(post("/demo/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role.name() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private LossReport reportDescribedAs(String description) {
        List<LossReport> found = lossReports.findAll().stream()
                .filter(report -> description.equals(report.getDescription())).toList();
        assertThat(found).hasSize(1);
        return found.getFirst();
    }

    private List<Integer> scoresOf(LossReport report) {
        return jdbc.queryForList("SELECT score FROM match_suggestions WHERE loss_report_id = ?", Integer.class,
                report.getId());
    }
}
