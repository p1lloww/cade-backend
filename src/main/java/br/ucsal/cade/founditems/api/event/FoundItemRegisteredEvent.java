package br.ucsal.cade.founditems.api.event;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Um objeto achado foi cadastrado (o {@code objeto.cadastrado} da seção 7.2 dos requisitos). Publicado na transação
 * do cadastro; o cruzamento com as perdas abertas ouve depois do commit (CRU), sem atrasar o funcionário.
 *
 * <p>Leva só o necessário para o cruzamento achar as perdas candidatas; os dados reservados do objeto não saem do
 * módulo (RN03).
 *
 * @param foundItemId id do objeto
 * @param categoryId  id da categoria
 * @param foundOn     data em que o objeto foi achado
 */
public record FoundItemRegisteredEvent(UUID foundItemId, UUID categoryId, LocalDate foundOn) {

    public FoundItemRegisteredEvent {
        Objects.requireNonNull(foundItemId);
        Objects.requireNonNull(categoryId);
        Objects.requireNonNull(foundOn);
    }
}
