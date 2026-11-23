package br.ucsal.cade.founditems.persistence.item;

import br.ucsal.cade.founditems.persistence.status.FoundItemStatus;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.RiskLevel;
import br.ucsal.cade.shared.exception.BadRequestException;
import br.ucsal.cade.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A correção dos dados do objeto (ACH-07). */
class FoundItemUpdateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);
    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");
    private static final Category HEADPHONES = new Category("Fone", RiskLevel.MEDIUM);
    private static final Category PHONE = new Category("Celular", RiskLevel.HIGH);

    @Test
    void correctsTheFieldsAndReportsWhatChangedBeforeAndAfter() {
        FoundItem item = register();

        List<String> changes = item.update(PHONE, "Bloco 100", "Sala 105", TODAY.minusDays(1), "Caixa Especial",
                RiskLevel.HIGH, "iPhone preto", "Capa azul", TODAY);

        assertThat(changes).containsExactly("categoria: Fone → Celular", "bloco: Bloco 400 → Bloco 100",
                "sala: Sala 421 → Sala 105", "data: " + TODAY.minusDays(2) + " → " + TODAY.minusDays(1),
                "caixa: Caixa Novembro/2026 → Caixa Especial", "risco: Médio → Alto",
                "descrição interna alterada", "detalhes reservados alterados");
        assertThat(item.getCategory()).isSameAs(PHONE);
        assertThat(item.getBlock()).isEqualTo("Bloco 100");
        assertThat(item.getRisk()).isEqualTo(RiskLevel.HIGH);
        assertThat(item.getInternalDescription()).isEqualTo("iPhone preto");
    }

    @Test
    void theTextsChangedAppearOnlyByNameInTheChangesNeverTheirContent() {
        FoundItem item = register();

        List<String> changes = item.update(HEADPHONES, "Bloco 400", "Sala 421", TODAY.minusDays(2), null, null,
                "Fone azul", "Adesivo de coração", TODAY);

        assertThat(changes).containsExactly("descrição interna alterada", "detalhes reservados alterados");
        assertThat(String.join(" ", changes)).doesNotContain("azul").doesNotContain("coração");
    }

    @Test
    void theRiskCanGoDownButNeverBelowTheCategoryAndADocumentStaysADocument() {
        FoundItem item = register();
        item.update(HEADPHONES, "Bloco 400", "Sala 421", TODAY.minusDays(2), null, RiskLevel.HIGH, null, null, TODAY);
        assertThat(item.getRisk()).isEqualTo(RiskLevel.HIGH);

        List<String> down = item.update(HEADPHONES, "Bloco 400", "Sala 421", TODAY.minusDays(2), null, null, null,
                null, TODAY);
        assertThat(down).contains("risco: Alto → Médio");

        assertThatThrownBy(() -> item.update(HEADPHONES, "Bloco 400", "Sala 421", TODAY.minusDays(2), null,
                RiskLevel.LOW, null, null, TODAY)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> item.update(HEADPHONES, "Bloco 400", "Sala 421", TODAY.minusDays(2), null,
                RiskLevel.DOCUMENT, null, null, TODAY)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void onlyAvailableAndUnclaimedItemsCanBeCorrected() {
        for (FoundItemStatus status : List.of(FoundItemStatus.AVAILABLE, FoundItemStatus.UNCLAIMED)) {
            FoundItem item = register();
            ReflectionTestUtils.setField(item, "status", status);
            assertThat(item.update(HEADPHONES, "Bloco 500", "Sala 421", TODAY.minusDays(2), null, null, null,
                    null, TODAY)).as(status.code()).hasSize(3);
        }
        for (FoundItemStatus locked : List.of(FoundItemStatus.RESERVED, FoundItemStatus.RETURNED,
                FoundItemStatus.DISPOSED)) {
            FoundItem item = register();
            ReflectionTestUtils.setField(item, "status", locked);
            assertThatThrownBy(() -> item.update(HEADPHONES, "Bloco 500", "Sala 421", TODAY.minusDays(2), null, null,
                    null, null, TODAY)).as(locked.code()).isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("corrigir um objeto");
            assertThat(item.getBlock()).isEqualTo("Bloco 400");
        }
    }

    private static FoundItem register() {
        return FoundItem.register(new NewFoundItem(HEADPHONES, "Bloco 400", "Sala 421", TODAY.minusDays(2), null,
                null, "Fone preto", "Adesivo de estrela"), "photo-key", UUID.randomUUID(), NOW, TODAY);
    }
}
