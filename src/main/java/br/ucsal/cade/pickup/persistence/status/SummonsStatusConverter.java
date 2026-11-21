package br.ucsal.cade.pickup.persistence.status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class SummonsStatusConverter implements AttributeConverter<SummonsStatus, String> {

    @Override
    public String convertToDatabaseColumn(SummonsStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public SummonsStatus convertToEntityAttribute(String code) {
        return code == null ? null : SummonsStatus.fromCode(code);
    }
}
