package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.shared.category.RiskLevel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Pedido de cadastro de um objeto achado, como o funcionário enviou.
 *
 * @param storageBox caixa escolhida; nula ou vazia para usar a sugerida pela data (RF05)
 * @param risk       risco escolhido; nulo para usar o padrão da categoria (RN04)
 */
public record RegisterFoundItem(byte[] photo, UUID categoryId, String block, String room, LocalDate foundOn,
                                String storageBox, RiskLevel risk, String internalDescription,
                                String reservedDetails) {

    public RegisterFoundItem {
        Objects.requireNonNull(photo);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(block);
        Objects.requireNonNull(room);
        Objects.requireNonNull(foundOn);
    }
}
