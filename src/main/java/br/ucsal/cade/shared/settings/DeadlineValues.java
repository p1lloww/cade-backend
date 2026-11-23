package br.ucsal.cade.shared.settings;

import java.time.Duration;

/**
 * Os três prazos, em dias inteiros.
 *
 * @param storageDays        guarda até o objeto virar não reclamado (RN16)
 * @param summonsDays        retirada depois da convocação (RN15)
 * @param reportValidityDays validade do registro de perda (RN14)
 */
public record DeadlineValues(int storageDays, int summonsDays, int reportValidityDays) {

    /** Os valores iniciais do projeto (seção 7 dos requisitos): 90, 7 e 30 dias. */
    public static final DeadlineValues INITIAL = new DeadlineValues(90, 7, 30);

    public Duration storagePeriod() {
        return Duration.ofDays(storageDays);
    }

    public Duration summonsPeriod() {
        return Duration.ofDays(summonsDays);
    }

    public Duration reportValidity() {
        return Duration.ofDays(reportValidityDays);
    }
}
