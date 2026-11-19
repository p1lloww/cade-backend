package br.ucsal.cade.lossreports.persistence.status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class LossReportStatusConverter implements AttributeConverter<LossReportStatus, String> {

    @Override
    public String convertToDatabaseColumn(LossReportStatus status) {
        return status == null ? null : status.code();
    }

    @Override
    public LossReportStatus convertToEntityAttribute(String code) {
        return code == null ? null : LossReportStatus.fromCode(code);
    }
}
