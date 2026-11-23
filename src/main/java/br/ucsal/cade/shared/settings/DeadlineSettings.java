package br.ucsal.cade.shared.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Os prazos que o administrador ajusta sem mudar código (RF45, RNF11): a guarda até virar não reclamado (RN16), a
 * retirada depois da convocação (RN15) e a validade do registro de perda (RN14). Uma linha só (id 1); sem ela, valem os
 * valores iniciais do projeto. Fica no núcleo compartilhado porque achados, perdas e retirada leem os mesmos prazos e
 * esses módulos não podem depender um do outro (D10).
 */
@Entity
@Table(name = "deadline_settings")
public class DeadlineSettings {

    public static final int SINGLE_ID = 1;

    @Id
    private Integer id = SINGLE_ID;

    @Column(nullable = false)
    private int storagePeriodDays;

    @Column(nullable = false)
    private int summonsPeriodDays;

    @Column(nullable = false)
    private int reportValidityDays;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private UUID updatedBy;

    protected DeadlineSettings() {
    }

    public DeadlineSettings(DeadlineValues values, Instant updatedAt, UUID updatedBy) {
        change(values, updatedAt, updatedBy);
    }

    public void change(DeadlineValues values, Instant updatedAt, UUID updatedBy) {
        this.storagePeriodDays = values.storageDays();
        this.summonsPeriodDays = values.summonsDays();
        this.reportValidityDays = values.reportValidityDays();
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public DeadlineValues values() {
        return new DeadlineValues(storagePeriodDays, summonsPeriodDays, reportValidityDays);
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }
}
