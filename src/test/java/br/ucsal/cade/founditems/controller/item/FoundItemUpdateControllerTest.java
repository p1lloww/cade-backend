package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.founditems.api.event.FoundItemUpdatedEvent;
import br.ucsal.cade.founditems.service.item.FoundItemService;
import br.ucsal.cade.founditems.service.item.RegisterFoundItem;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A correção dos dados do objeto achado (ACH-07): rota, versão, situações, histórico e perfis. */
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemUpdateControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private FoundItemService service;

    @Autowired
    private AuditRecordRepository auditRecords;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID staff;
    private UUID category;
    private UUID item;

    @BeforeEach
    void setUp() {
        staff = UUID.randomUUID();
        category = categories.saveAndFlush(new Category("Teste " + UUID.randomUUID(), RiskLevel.MEDIUM)).getId();
        item = service.register(new RegisterFoundItem(TestPhotos.jpeg(), category, "Bloco 400", "Sala 421",
                LocalDate.now(clock).minusDays(2), null, null, "Fone preto", "Adesivo de estrela"), staff).getId();
    }

    @Test
    void theStaffCorrectsTheItemAndTheHistoryKeepsTheReasonAndWhatChanged() throws Exception {
        mvc.perform(update(item, body(0, "Bloco 100", "Sala 105", "Local anotado errado na etiqueta"), staffToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.block").value("Bloco 100"))
                .andExpect(jsonPath("$.room").value("Sala 105"))
                .andExpect(jsonPath("$.version").value(1));

        assertThat(events.stream(FoundItemUpdatedEvent.class)).containsExactly(
                new FoundItemUpdatedEvent(item, category, LocalDate.now(clock).minusDays(2)));
        assertThat(auditRecords.findAll()).filteredOn(record -> record.getEntityId().equals(item)
                && record.getAction().equals("FOUND_ITEM_UPDATED")).singleElement().satisfies(record -> {
            assertThat(record.getActorId()).isEqualTo(staff);
            assertThat(record.getReason()).startsWith("Local anotado errado na etiqueta Alterado: ")
                    .contains("bloco: Bloco 400 → Bloco 100", "sala: Sala 421 → Sala 105")
                    .doesNotContain("estrela").doesNotContain("Fone preto");
        });
    }

    @Test
    void aReservedOrFinishedItemCannotBeCorrected() throws Exception {
        for (String locked : new String[]{"RESERVED", "RETURNED", "DISPOSED"}) {
            jdbc.update("UPDATE found_items SET status = ? WHERE id = ?", locked, item);

            mvc.perform(update(item, body(0, "Bloco 999", "Sala 9", "Tentativa"), staffToken()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(startsWith("Não é possível corrigir um objeto")));

            assertThat(jdbc.queryForObject("SELECT block FROM found_items WHERE id = ?", String.class, item))
                    .as(locked).isEqualTo("Bloco 400");
        }
    }

    @Test
    void anOldVersionIsRefusedWithConflictAndNothingIsWritten() throws Exception {
        // Outra alteração (por exemplo, a reserva do objeto) foi gravada depois de o funcionário abrir a tela.
        jdbc.update("UPDATE found_items SET version = version + 1 WHERE id = ?", item);

        mvc.perform(update(item, body(0, "Bloco 999", "Sala 9", "Tentativa"), staffToken()))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT block FROM found_items WHERE id = ?", String.class, item))
                .isEqualTo("Bloco 400");
    }

    private MockHttpServletRequestBuilder update(UUID id, String json, String bearer) {
        return put("/found-items/{id}", id).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private String body(long version, String block, String room, String reason) {
        return """
                {"categoryId": "%s", "block": "%s", "room": "%s", "foundOn": "%s", "storageBox": null, "risk": null,
                 "internalDescription": "Fone preto", "reservedDetails": "Adesivo de estrela", "reason": "%s",
                 "version": %d}""".formatted(category, block, room, LocalDate.now(clock).minusDays(2), reason, version);
    }

    private String staffToken() {
        return "Bearer " + TestTokens.token(encoder, clock.instant(), claims -> claims.subject(staff.toString())
                .claim(TokenClaims.ROLE, Role.STAFF.name()));
    }
}
