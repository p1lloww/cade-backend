package br.ucsal.cade.retention.persistence.disposal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * O destino dado a um objeto não reclamado (RF38): para onde foi, quando e quem decidiu. Um objeto só é destinado uma
 * vez. Guarda o id do objeto, sem chave estrangeira para outro módulo (D10).
 */
@Entity
@Table(name = "disposals")
public class Disposal {

    public static final int MAX_NOTE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID foundItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisposalDestination destination;

    @Column(length = MAX_NOTE_LENGTH)
    private String note;

    @Column(nullable = false)
    private Instant disposedAt;

    @Column(nullable = false)
    private UUID disposedBy;

    protected Disposal() {
    }

    public Disposal(UUID foundItemId, DisposalDestination destination, String note, Instant disposedAt,
                    UUID disposedBy) {
        this.foundItemId = Objects.requireNonNull(foundItemId);
        this.destination = Objects.requireNonNull(destination);
        this.note = note == null || note.isBlank() ? null : note.strip();
        this.disposedAt = Objects.requireNonNull(disposedAt);
        this.disposedBy = Objects.requireNonNull(disposedBy);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFoundItemId() {
        return foundItemId;
    }

    public DisposalDestination getDestination() {
        return destination;
    }

    public String getNote() {
        return note;
    }

    public Instant getDisposedAt() {
        return disposedAt;
    }

    public UUID getDisposedBy() {
        return disposedBy;
    }
}
