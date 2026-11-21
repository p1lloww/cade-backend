package br.ucsal.cade.lossreports.api;

import java.util.Objects;
import java.util.UUID;

/** O registro recém-convocado e o estudante dono dele, que recebe a convocação. */
public record SummonedLossReport(UUID id, UUID studentId) {

    public SummonedLossReport {
        Objects.requireNonNull(id);
        Objects.requireNonNull(studentId);
    }
}
