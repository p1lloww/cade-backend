package br.ucsal.cade.pickup.persistence.pickup;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * O que o funcionário conferiu no balcão antes de entregar (RF29), no lugar do formulário de papel: o documento com
 * foto, a matrícula (RN10) e a verificação exigida pelo risco (RN09).
 *
 * @param notes observação livre do funcionário; opcional
 */
@Embeddable
public record CounterCheck(
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) IdDocumentType documentType,
        @Column(nullable = false, length = 30) String documentNumber,
        @Column(nullable = false, length = 20) String enrollment,
        @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) VerificationMethod verification,
        @Column(length = 500) String notes) {
}
