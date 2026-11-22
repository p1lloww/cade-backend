package br.ucsal.cade.notifications.persistence.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Um aviso na caixa de saída (Transactional Outbox, D30). A linha é gravada na mesma transação da ação que gerou o
 * aviso, então existe se e somente se a ação foi gravada. O envio vem depois, fora dessa transação.
 *
 * <p>Guarda o texto já pronto, mas não o endereço: o e-mail é buscado na conta na hora do envio, para que nada que
 * possa falhar rode dentro da transação da ação. Por isso o texto nunca leva segredo; o código de login não passa por
 * aqui.
 *
 * <p>O par (tipo, referência) é único: o mesmo aviso nunca é gravado duas vezes.
 */
@Entity
@Table(name = "notifications", uniqueConstraints = @UniqueConstraint(columnNames = {"kind", "reference_id"}))
public class Notification {

    public static final int MAX_SUBJECT_LENGTH = 200;
    public static final int MAX_BODY_LENGTH = 4000;

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 40)
    private NotificationKind kind;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Column(nullable = false, updatable = false)
    private UUID recipientId;

    @Column(nullable = false, updatable = false, length = MAX_SUBJECT_LENGTH)
    private String subject;

    @Column(nullable = false, updatable = false, length = MAX_BODY_LENGTH)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant sentAt;

    protected Notification() {
    }

    public void markSent(Instant at) {
        attempts++;
        status = NotificationStatus.SENT;
        sentAt = at;
    }

    /**
     * Mais uma tentativa que falhou. A espera cresce a cada falha (1×, 2×, 3× o intervalo), para não insistir num
     * servidor fora do ar; na última tentativa, o aviso é abandonado.
     */
    public void markFailed(Instant at, int maxAttempts, Duration retryDelay) {
        attempts++;
        if (attempts >= maxAttempts) {
            status = NotificationStatus.ABANDONED;
        } else {
            nextAttemptAt = at.plus(retryDelay.multipliedBy(attempts));
        }
    }

    /** O destinatário não tem mais conta: não adianta tentar de novo. */
    public void abandon() {
        status = NotificationStatus.ABANDONED;
    }

    public UUID getId() {
        return id;
    }

    public NotificationKind getKind() {
        return kind;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public UUID getRecipientId() {
        return recipientId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
