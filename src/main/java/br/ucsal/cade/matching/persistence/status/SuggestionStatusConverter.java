package br.ucsal.cade.matching.persistence.status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SuggestionStatusConverter implements AttributeConverter<SuggestionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SuggestionStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public SuggestionStatus convertToEntityAttribute(String code) {
        return code == null ? null : SuggestionStatus.fromCode(code);
    }
}
