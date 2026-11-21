package br.ucsal.cade.pickup.persistence.pickup;

import br.ucsal.cade.pickup.persistence.summons.Summons;
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
 * A retirada registrada no balcão (RF29): quem retirou, o que foi conferido, o funcionário, a data e a hora. Nunca
 * muda depois de gravada, como o histórico (RN19).
 *
 * <p>Objeto e estudante são referências por id, sem chave estrangeira: as tabelas são de outros módulos (D10).
 */
@Entity
@Table(name = "pickups")
public class Pickup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID summonsId;

    @Column(nullable = false, updatable = false)
    private UUID foundItemId;

    @Column(nullable = false, updatable = false)
    private UUID studentId;

    @Embedded
    private CounterCheck check;

    @Column(nullable = false, updatable = false)
    private UUID handedOverBy;

    @Column(nullable = false, updatable = false)
    private Instant handedOverAt;

    protected Pickup() {
    }

    private Pickup(Summons summons, CounterCheck check, UUID staffId, Instant at) {
        this.summonsId = Objects.requireNonNull(summons.getId());
        this.foundItemId = summons.getFoundItemId();
        this.studentId = summons.getStudentId();
        this.check = Objects.requireNonNull(check);
        this.handedOverBy = Objects.requireNonNull(staffId);
        this.handedOverAt = Objects.requireNonNull(at);
    }

    /** A retirada do objeto de uma convocação, pelo estudante convocado (RN11). */
    public static Pickup of(Summons summons, CounterCheck check, UUID staffId, Instant at) {
        return new Pickup(summons, check, staffId, at);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSummonsId() {
        return summonsId;
    }

    public UUID getFoundItemId() {
        return foundItemId;
    }

    public UUID getStudentId() {
        return studentId;
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
