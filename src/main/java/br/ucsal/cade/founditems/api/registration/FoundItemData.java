package br.ucsal.cade.founditems.api.registration;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * O que um cadastro de objeto achado precisa, para quem cadastra por código (o modo demonstração, RF47), sem passar
 * pela rota do funcionário. A caixa e o risco seguem a sugestão da data e o padrão da categoria (RF05, RN04).
 *
 * @param internalDescription descrição interna, só para funcionários (RF06); pode ser nula
 * @param reservedDetails     detalhes reservados, só para funcionários (RF06); podem ser nulos
 */
public record FoundItemData(byte[] photo, UUID categoryId, String block, String room, LocalDate foundOn,
                            String internalDescription, String reservedDetails) {

    public FoundItemData {
        Objects.requireNonNull(photo);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(block);
        Objects.requireNonNull(room);
        Objects.requireNonNull(foundOn);
    }
}
