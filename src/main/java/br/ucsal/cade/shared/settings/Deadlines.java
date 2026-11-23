package br.ucsal.cade.shared.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * Os prazos em vigor (RF45): os que o administrador configurou ou, se nunca mexeu, os valores iniciais. Lidos na hora
 * de usar, para uma mudança valer logo, sem reiniciar. Quem muda é o módulo de administração.
 *
 * <p>Uma mudança vale para o que vem depois: a convocação e o registro já criados guardam o prazo com que nasceram.
 */
@Service
public class Deadlines {

    private final DeadlineSettingsRepository settings;

    Deadlines(DeadlineSettingsRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public DeadlineValues current() {
        return settings.findById(DeadlineSettings.SINGLE_ID).map(DeadlineSettings::values)
                .orElse(DeadlineValues.INITIAL);
    }

    /** Guarda até virar não reclamado (RN16). */
    public Duration storagePeriod() {
        return current().storagePeriod();
    }

    /** Retirada depois da convocação (RN15). */
    public Duration summonsPeriod() {
        return current().summonsPeriod();
    }

    /** Validade do registro de perda (RN14). */
    public Duration reportValidity() {
        return current().reportValidity();
    }
}
