package br.ucsal.cade.founditems.controller.item;

import br.ucsal.cade.TestcontainersConfiguration;
import br.ucsal.cade.administration.persistence.audit.AuditRecord;
import br.ucsal.cade.administration.persistence.audit.AuditRecordRepository;
import br.ucsal.cade.founditems.api.event.FoundItemRegisteredEvent;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.security.Role;
import br.ucsal.cade.shared.security.TestTokens;
import br.ucsal.cade.shared.storage.PhotoProperties;
import br.ucsal.cade.shared.storage.TestPhotos;
import br.ucsal.cade.shared.time.MutableClock;
import br.ucsal.cade.shared.time.MutableClockConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, MutableClockConfiguration.class})
class FoundItemControllerTest {

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
    private PhotoProperties photoProperties;

    @Autowired
    private ApplicationEvents events;

    @Test
    void theStaffRegistersAnItemWithTheSuggestedBoxAndTheCategoryRisk() throws Exception {
        String body = register(form(headphones()).param("internalDescription", "Fone JBL preto")
                        .param("reservedDetails", "Adesivo de estrela"), Role.STAFF)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/found-items/")))
                .andExpect(jsonPath("$.categoryName").value("Fone"))
                .andExpect(jsonPath("$.block").value("Bloco 400"))
                .andExpect(jsonPath("$.room").value("Sala 421"))
                .andExpect(jsonPath("$.foundOn").value(lastWeek().toString()))
                .andExpect(jsonPath("$.storageBox").value("Caixa " + monthName(lastWeek()) + "/" + lastWeek().getYear()))
                .andExpect(jsonPath("$.risk").value("MEDIUM"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.statusLabel").value("Disponível"))
                .andExpect(jsonPath("$.internalDescription").value("Fone JBL preto"))
                .andExpect(jsonPath("$.reservedDetails").value("Adesivo de estrela"))
                .andReturn().getResponse().getContentAsString();

        UUID id = UUID.fromString(JsonPath.read(body, "$.id"));
        assertThat(events.stream(FoundItemRegisteredEvent.class))
                .containsExactly(new FoundItemRegisteredEvent(id, headphones(), lastWeek()));
        assertThat(auditRecords.findAll()).filteredOn(record -> record.getEntityId().equals(id))
                .singleElement()
                .satisfies(record -> {
                    assertThat(record.getAction()).isEqualTo("FOUND_ITEM_REGISTERED");
                    assertThat(record.getActorId()).isEqualTo(UUID.fromString(TestTokens.SUBJECT));
                });
    }

    @Test
    void theAdminCanAlsoRegister() throws Exception {
        register(form(headphones()), Role.ADMIN).andExpect(status().isCreated());
    }

    @Test
    void theStaffCanChooseTheBoxAndRaiseTheRisk() throws Exception {
        register(form(headphones()).param("storageBox", "Caixa Eletrônicos").param("risk", "HIGH"), Role.STAFF)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.storageBox").value("Caixa Eletrônicos"))
                .andExpect(jsonPath("$.risk").value("HIGH"));
    }

    @Test
    void aStudentCannotRegisterAndAVisitorMustLogIn() throws Exception {
        register(form(headphones()), Role.STUDENT).andExpect(status().isForbidden());
        mvc.perform(form(headphones())).andExpect(status().isUnauthorized());
    }

    @Test
    void asksForEachRequiredField() throws Exception {
        MockMultipartHttpServletRequestBuilder empty = multipart("/found-items");

        register(empty, Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.photo").value("Envie a foto do objeto."))
                .andExpect(jsonPath("$.errors.categoryId").value("Escolha a categoria."))
                .andExpect(jsonPath("$.errors.block").value("Informe o bloco."))
                .andExpect(jsonPath("$.errors.room").value("Informe a sala."))
                .andExpect(jsonPath("$.errors.foundOn").value("Informe a data em que o objeto foi achado."));
    }

    @Test
    void answersAMalformedDateInPortuguese() throws Exception {
        register(form(headphones(), "04/11/2026"), Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.foundOn").value("Valor inválido."));
    }

    @Test
    void refusesAFileThatIsNotAPhoto() throws Exception {
        MockMultipartHttpServletRequestBuilder pdf = multipart("/found-items")
                .file(new MockMultipartFile("photo", "fone.jpg", "image/jpeg", TestPhotos.pdf()));
        fields(pdf, headphones(), lastWeek().toString());

        register(pdf, Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A foto precisa ser JPEG, PNG ou WebP."));
    }

    @Test
    void refusesAnUnknownCategory() throws Exception {
        register(form(UUID.randomUUID()), Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Categoria não encontrada."));
    }

    @Test
    void doesNotKeepThePhotoWhenTheRegistrationFails() throws Exception {
        long before = photoCount();

        register(form(headphones()).param("risk", "LOW"), Role.STAFF)
                .andExpect(status().isBadRequest());
        register(form(headphones(), LocalDate.now(clock).plusDays(1).toString()), Role.STAFF)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A data em que o objeto foi achado não pode ser no futuro."));

        assertThat(photoCount()).isEqualTo(before);
    }

    @Test
    void theStaffSeesTheItemAndThePhoto() throws Exception {
        String body = register(form(headphones()), Role.STAFF).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/found-items/" + id).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.photoPath").value("/found-items/" + id + "/photo"));
        mvc.perform(get("/found-items/" + id + "/photo").header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(TestPhotos.jpeg()));
    }

    @Test
    void neverShowsTheItemOrThePhotoToStudents() throws Exception {
        String body = register(form(headphones()), Role.STAFF).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(get("/found-items/" + id).header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/found-items/" + id + "/photo").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/found-items/" + id + "/photo")).andExpect(status().isUnauthorized());
    }

    @Test
    void answersAnUnknownItemWith404() throws Exception {
        mvc.perform(get("/found-items/" + UUID.randomUUID()).header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Objeto não encontrado."));
    }

    @Test
    void suggestsTheBoxForADate() throws Exception {
        mvc.perform(get("/found-items/storage-box-suggestion").param("date", "2026-11-04")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageBox").value("Caixa Novembro/2026"));
        mvc.perform(get("/found-items/storage-box-suggestion").param("date", "ontem")
                        .header("Authorization", bearer(Role.STAFF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Confira os campos enviados."));
    }

    private ResultActions register(MockMultipartHttpServletRequestBuilder request, Role role) throws Exception {
        return mvc.perform(request.header("Authorization", bearer(role)));
    }

    private MockMultipartHttpServletRequestBuilder form(UUID categoryId) {
        return form(categoryId, lastWeek().toString());
    }

    private MockMultipartHttpServletRequestBuilder form(UUID categoryId, String foundOn) {
        MockMultipartHttpServletRequestBuilder request = multipart("/found-items")
                .file(new MockMultipartFile("photo", "fone.jpg", "image/jpeg", TestPhotos.jpeg()));
        return fields(request, categoryId, foundOn);
    }

    private MockMultipartHttpServletRequestBuilder fields(MockMultipartHttpServletRequestBuilder request,
                                                          UUID categoryId, String foundOn) {
        request.param("categoryId", categoryId.toString())
                .param("block", "Bloco 400")
                .param("room", "Sala 421")
                .param("foundOn", foundOn);
        return request;
    }

    private UUID headphones() {
        return categories.findByNameIgnoreCase("Fone").map(Category::getId).orElseThrow();
    }

    private LocalDate lastWeek() {
        return LocalDate.now(clock).minusDays(7);
    }

    private static String monthName(LocalDate date) {
        String[] months = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro",
                "Outubro", "Novembro", "Dezembro"};
        return months[date.getMonthValue() - 1];
    }

    private String bearer(Role role) {
        return "Bearer " + TestTokens.forRole(encoder, clock.instant(), role);
    }

    private long photoCount() throws Exception {
        Path directory = photoProperties.directory();
        if (!Files.isDirectory(directory)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.count();
        }
    }
}
