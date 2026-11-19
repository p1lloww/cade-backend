package br.ucsal.cade.lossreports.persistence.status;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Grava a {@link LossReportStatus} como texto ({@code OPEN}, {@code AWAITING_INFO}...) e lê de volta. Com
 * {@code autoApply}, todo campo {@code LossReportStatus} de uma entidade usa este conversor sem anotação nenhuma.
 */
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
