package br.ucsal.cade.founditems.api.event;

import java.time.LocalDate;
import java.util.UUID;

/**
 * O funcionário corrigiu os dados de um objeto achado (ACH-07). Publicado na transação da correção, só quando algum
 * campo mudou. Como o {@link FoundItemRegisteredEvent}, não leva foto, descrição interna nem detalhes reservados (RN03).
 */
public record FoundItemUpdatedEvent(UUID foundItemId, UUID categoryId, LocalDate foundOn) {
}
