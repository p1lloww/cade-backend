package br.ucsal.cade.lossreports.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Um registro de perda perto de vencer (RF35).
 *
 * @param description o que o próprio estudante escreveu, para ele reconhecer o registro no e-mail
 */
public record ExpiringLossReport(UUID lossReportId, UUID studentId, String description, Instant expiresAt) {

    public ExpiringLossReport {
        Objects.requireNonNull(lossReportId);
        Objects.requireNonNull(studentId);
        Objects.requireNonNull(description);
        Objects.requireNonNull(expiresAt);
    }
}
