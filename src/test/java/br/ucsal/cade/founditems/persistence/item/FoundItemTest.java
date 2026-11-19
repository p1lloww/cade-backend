package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoundItemTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final UUID STAFF = UUID.randomUUID();
    private static final Category HEADPHONES = new Category("Fone", RiskLevel.MEDIUM);
    private static final Category DOCUMENTS = new Category("Documentos", RiskLevel.DOCUMENT);

    @Test
    void startsAvailableWithTheSuggestedBoxAndTheCategoryRisk() {
        FoundItem item = register(data(HEADPHONES, null, null));

        assertThat(item.getStatus()).isEqualTo(FoundItemStatus.AVAILABLE);
        assertThat(item.getStorageBox()).isEqualTo("Caixa Novembro/2026");
        assertThat(item.getRisk()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(item.getRegisteredBy()).isEqualTo(STAFF);
        assertThat(item.getRegisteredAt()).isEqualTo(NOW);
        assertThat(item.getPhotoKey()).isEqualTo("photo.jpg");
    }

    @Test
    void keepsTheBoxChosenByTheStaff() {
        FoundItem item = register(data(HEADPHONES, "  Caixa Eletrônicos  ", null));

        assertThat(item.getStorageBox()).isEqualTo("Caixa Eletrônicos");
    }

    @Test
    void usesTheSuggestedBoxWhenTheChosenOneIsBlank() {
        FoundItem item = register(data(HEADPHONES, "   ", null));

        assertThat(item.getStorageBox()).isEqualTo("Caixa Novembro/2026");
    }

    @Test
    void theStaffCanRaiseTheRisk() {
        FoundItem item = register(data(HEADPHONES, null, RiskLevel.HIGH));

        assertThat(item.getRisk()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void theStaffCannotLowerTheRiskOfTheCategory() {
        assertThatThrownBy(() -> register(data(HEADPHONES, null, RiskLevel.LOW)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Fone")
                .hasMessageContaining("Baixo");
    }

    @Test
    void aDocumentStaysADocument() {
        assertThat(register(data(DOCUMENTS, null, null)).getRisk()).isEqualTo(RiskLevel.DOCUMENT);
        assertThatThrownBy(() -> register(data(DOCUMENTS, null, RiskLevel.HIGH)))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> register(data(HEADPHONES, null, RiskLevel.DOCUMENT)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void acceptsAnItemFoundToday() {
        NewFoundItem today = new NewFoundItem(HEADPHONES, "Bloco 400", "Sala 421", TODAY, null, null, null, null);

        assertThat(register(today).getFoundOn()).isEqualTo(TODAY);
    }

    @Test
    void refusesAnItemFoundInTheFuture() {
        NewFoundItem tomorrow = new NewFoundItem(HEADPHONES, "Bloco 400", "Sala 421", TODAY.plusDays(1), null, null,
                null, null);

        assertThatThrownBy(() -> register(tomorrow))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(FoundItem.FUTURE_DATE_MESSAGE);
    }

    @Test
    void stripsTheTextsAndDropsTheBlankOptionalOnes() {
        NewFoundItem data = new NewFoundItem(HEADPHONES, " Bloco 400 ", " Sala 421 ", TODAY, null, null,
                "  Fone JBL preto ", "   ");

        FoundItem item = register(data);

        assertThat(item.getBlock()).isEqualTo("Bloco 400");
        assertThat(item.getRoom()).isEqualTo("Sala 421");
        assertThat(item.getInternalDescription()).isEqualTo("Fone JBL preto");
        assertThat(item.getReservedDetails()).isNull();
    }

    private static NewFoundItem data(Category category, String storageBox, RiskLevel risk) {
        return new NewFoundItem(category, "Bloco 400", "Sala 421", LocalDate.of(2026, 11, 4), storageBox, risk,
                null, null);
    }

    private static FoundItem register(NewFoundItem data) {
        return FoundItem.register(data, "photo.jpg", STAFF, NOW, TODAY);
    }
}
