package br.ucsal.cade.pickup.persistence.walkin;

import br.ucsal.cade.pickup.persistence.pickup.CounterCheck;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A retirada pelo atalho de balcão, sem registro de perda nem convocação (RF32). Nunca muda depois de gravada, como o
 * histórico (RN19).
 *
 * <p>Fica separada da {@code Pickup}, que sempre tem convocação e estudante. A matrícula é opcional aqui porque quem
 * não tem conta não tem matrícula (RN20). O objeto é referência por id, sem chave estrangeira (D10), e sai da sala uma
 * vez só.
 */
@Entity
@Table(name = "walk_in_pickups")
public class WalkInPickup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID foundItemId;

    @Embedded
    private WalkInHolder holder;

    @Embedded
    @AttributeOverride(name = "enrollment", column = @Column(name = "enrollment", length = 20))
    private CounterCheck check;

    @Column(nullable = false, updatable = false)
    private UUID handedOverBy;

    @Column(nullable = false, updatable = false)
    private Instant handedOverAt;

    protected WalkInPickup() {
    }

    private WalkInPickup(UUID foundItemId, WalkInHolder holder, CounterCheck check, UUID staffId, Instant at) {
        this.foundItemId = Objects.requireNonNull(foundItemId);
        this.holder = Objects.requireNonNull(holder);
        this.check = Objects.requireNonNull(check);
        this.handedOverBy = Objects.requireNonNull(staffId);
        this.handedOverAt = Objects.requireNonNull(at);
    }

    public static WalkInPickup of(UUID foundItemId, WalkInHolder holder, CounterCheck check, UUID staffId,
                                  Instant at) {
        return new WalkInPickup(foundItemId, holder, check, staffId, at);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFoundItemId() {
        return foundItemId;
    }

    public WalkInHolder getHolder() {
        return holder;
    }

    public CounterCheck getCheck() {
        return check;
    }

    public UUID getHandedOverBy() {
        return handedOverBy;
    }

    public Instant getHandedOverAt() {
        return handedOverAt;
    }
}
