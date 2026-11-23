package br.ucsal.cade.pickup.persistence.pickup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * O estudante confirmou no próprio celular a retirada que o funcionário registrou no balcão, no lugar da assinatura no
 * papel (RF30). Só se grava, nunca se altera: a {@link Pickup} fica intacta (RN19), e uma retirada tem no máximo uma
 * confirmação (a coluna {@code pickupId} é única).
 */
@Entity
@Table(name = "pickup_confirmations")
public class PickupConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID pickupId;

    @Column(nullable = false, updatable = false)
    private UUID studentId;

    @Column(nullable = false, updatable = false)
    private Instant confirmedAt;

    protected PickupConfirmation() {
    }

    private PickupConfirmation(UUID pickupId, UUID studentId, Instant confirmedAt) {
        this.pickupId = Objects.requireNonNull(pickupId);
        this.studentId = Objects.requireNonNull(studentId);
        this.confirmedAt = Objects.requireNonNull(confirmedAt);
    }

    public static PickupConfirmation of(Pickup pickup, Instant at) {
        return new PickupConfirmation(pickup.getId(), pickup.getStudentId(), at);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPickupId() {
        return pickupId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }
}
