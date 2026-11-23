package br.ucsal.cade.lossreports.controller.report;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.lossreports.persistence.location.LossLocation;
import br.ucsal.cade.lossreports.service.report.LossReportService;
import br.ucsal.cade.lossreports.service.report.RegisterLossReport;
import br.ucsal.cade.matching.persistence.suggestion.MatchSuggestionRepository;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.security.jwt.TokenClaims;
import br.ucsal.cade.shared.storage.PhotoProperties;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A prova do registro de perda (RF15, PER-05): foto antiga, nota fiscal e número de série. */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class LossReportProofControllerTest {

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
    private FoundItemService foundItemService;

    @Autowired
    private MatchSuggestionRepository suggestions;

    @Autowired
    private PhotoProperties photoProperties;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID student;
    private UUID category;
    private UUID report;

    @BeforeEach
    void setUp() {
        student = UUID.randomUUID();
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.HIGH)).getId();
        report = lossReportService.register(new RegisterLossReport(category, "iPhone azul", null,
                List.of(LossLocation.of("Bloco 400", "Sala 421")), LocalDate.now(clock), null, null, null),
                student).getId();
    }

    @Test
    void attachesAPhotoAndASerialNumberAndTheStudentSeesThePath() throws Exception {
        mvc.perform(attach(report, "{\"serialNumber\":\" SN-123456 \"}", TestPhotos.png(), studentBearer(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proofSerialNumber").value("SN-123456"))
                .andExpect(jsonPath("$.proofPath").value("/loss-reports/" + report + "/proof"));

        assertThat(jdbc.queryForList("SELECT action FROM audit_records WHERE entity_id = ?", String.class, report))
                .contains("LOSS_REPORT_PROOF_ATTACHED");
        assertThat(jdbc.queryForList("SELECT reason FROM audit_records WHERE entity_id = ? AND action = ?",
                String.class, report, "LOSS_REPORT_PROOF_ATTACHED")).noneMatch(reason -> reason.contains("SN-"));
    }

    @Test
    void anotherStudentGetsNotFoundOnBothRoutes() throws Exception {
        mvc.perform(attach(report, null, TestPhotos.png(), studentBearer(student))).andExpect(status().isOk());
        String other = studentBearer(UUID.randomUUID());

        mvc.perform(attach(report, "{\"serialNumber\":\"SN-9\"}", null, other)).andExpect(status().isNotFound());
        mvc.perform(get("/loss-reports/{id}/proof", report).header("Authorization", other))
                .andExpect(status().isNotFound());
    }

    @Test
    void theProofCanBeAddedWhileSummonedButNotAfterTheReportIsOver() throws Exception {
        jdbc.update("UPDATE loss_reports SET status = 'SUMMONED' WHERE id = ?", report);
        mvc.perform(attach(report, "{\"serialNumber\":\"SN-2\"}", null, studentBearer(student)))
                .andExpect(status().isOk());

        for (String finished : new String[]{"COMPLETED", "EXPIRED", "CANCELLED", "REFUSED"}) {
            jdbc.update("UPDATE loss_reports SET status = ? WHERE id = ?", finished, report);
            mvc.perform(attach(report, "{\"serialNumber\":\"SN-3\"}", null, studentBearer(student)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(startsWith("Não é possível anexar prova a")));
        }
    }

    @Test
    void theOwnerAndTheStaffReadThePhotoWithoutCacheAndOthersCannot() throws Exception {
        mvc.perform(attach(report, null, TestPhotos.png(), studentBearer(student))).andExpect(status().isOk());

        for (String bearer : new String[]{studentBearer(student), bearer(Role.STAFF), bearer(Role.ADMIN)}) {
            mvc.perform(get("/loss-reports/{id}/proof", report).header("Authorization", bearer))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(content().bytes(TestPhotos.png()))
                    .andExpect(header().string("Cache-Control", "no-store, private"));
        }
        mvc.perform(get("/loss-reports/{id}/proof", report)).andExpect(status().isUnauthorized());
    }

    private MockMultipartHttpServletRequestBuilder attach(UUID id, String proofJson, byte[] file, String bearer) {
        MockMultipartHttpServletRequestBuilder request = multipart("/loss-reports/{id}/proof", id)
                .header("Authorization", bearer);
        if (proofJson != null) {
            request.file(new MockMultipartFile("proof", "", MediaType.APPLICATION_JSON_VALUE,
                    proofJson.getBytes(StandardCharsets.UTF_8)));
        }
        if (file != null) {
            request.file(new MockMultipartFile("file", "prova", MediaType.APPLICATION_OCTET_STREAM_VALUE, file));
        }
        return request;
    }

    private String studentBearer(UUID id) {
        return "Bearer " + TestTokens.token(encoder, clock.instant(), claims -> claims.subject(id.toString())
                .claim(TokenClaims.ROLE, Role.STUDENT.name()));
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }
}
