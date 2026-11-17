package br.ucsal.cade.founditems.persistence.status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Grava a {@link FoundItemStatus} como texto ({@code AVAILABLE}, {@code RESERVED}...) e lê de volta. Com
 * {@code autoApply}, todo campo {@code FoundItemStatus} de uma entidade usa este conversor sem anotação nenhuma.
 */
@Converter(autoApply = true)
public class FoundItemStatusConverter implements AttributeConverter<FoundItemStatus, String> {

    @Override
    public String convertToDatabaseColumn(FoundItemStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public FoundItemStatus convertToEntityAttribute(String code) {
        return code == null ? null : FoundItemStatus.fromCode(code);
    }
}
