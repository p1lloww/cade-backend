package br.ucsal.cade.founditems.service.item;

import br.ucsal.cade.shared.category.RiskLevel;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Pedido de correção dos dados de um objeto, como o funcionário enviou (ACH-07). Leva os mesmos campos do cadastro,
 * menos a foto, o motivo da correção e a versão que ele viu.
 *
 * @param storageBox caixa escolhida; nula ou vazia para usar a sugerida pela data (RF05)
 * @param risk       risco escolhido; nulo para usar o padrão da categoria (RN04)
 * @param reason     por que o dado foi corrigido; vai para o histórico (RF43)
 * @param version    a {@code version} do objeto na última vez que o funcionário o recebeu
 */
public record UpdateFoundItem(UUID categoryId, String block, String room, LocalDate foundOn, String storageBox,
                              RiskLevel risk, String internalDescription, String reservedDetails, String reason,
                              long version) {

    public UpdateFoundItem {
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(block);
        Objects.requireNonNull(room);
        Objects.requireNonNull(foundOn);
        Objects.requireNonNull(reason);
    }
}
