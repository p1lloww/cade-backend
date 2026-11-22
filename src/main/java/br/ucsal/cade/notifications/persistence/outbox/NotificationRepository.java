package br.ucsal.cade.notifications.persistence.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Grava o aviso Pendente, a menos que o mesmo tipo para a mesma referência já exista. Com o {@code ON CONFLICT},
     * um evento repetido ou duas rodadas da tarefa dos prazos não gravam nada e não falham.
     *
     * @return 1 se gravou, 0 se o aviso já existia
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO notifications (id, kind, reference_id, recipient_id, subject, body, status, attempts,
                                       created_at, next_attempt_at)
            VALUES (:id, :kind, :referenceId, :recipientId, :subject, :body, 'PENDING', 0, :now, :now)
            ON CONFLICT (kind, reference_id) DO NOTHING""")
    int insertIfAbsent(@Param("id") UUID id, @Param("kind") String kind, @Param("referenceId") UUID referenceId,
                       @Param("recipientId") UUID recipientId, @Param("subject") String subject,
                       @Param("body") String body, @Param("now") Instant now);

    /** Os Pendentes cuja hora de tentar já chegou, dos mais antigos para os mais novos. */
    @Query(nativeQuery = true, value = """
            SELECT id FROM notifications
            WHERE status = 'PENDING' AND next_attempt_at <= :now
            ORDER BY next_attempt_at
            LIMIT :limit""")
    List<UUID> findDueIds(@Param("now") Instant now, @Param("limit") int limit);

    /**
     * O aviso Pendente, travado até o fim da transação. O {@code SKIP LOCKED} faz quem chega depois (o envio logo
     * após o commit e a rodada do reenvio, ou dois servidores) receber vazio em vez de esperar: só um envia.
     */
    @Query(nativeQuery = true, value = """
            SELECT * FROM notifications
            WHERE id = :id AND status = 'PENDING'
            FOR UPDATE SKIP LOCKED""")
    Optional<Notification> lockPending(@Param("id") UUID id);
}
