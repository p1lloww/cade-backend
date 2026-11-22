package br.ucsal.cade.pickup.persistence.summons;

import br.ucsal.cade.pickup.persistence.status.SummonsStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SummonsRepository extends JpaRepository<Summons, UUID> {

    /** As que vencem primeiro no topo, para o balcão atender antes quem está perto do prazo (RN15). */
    List<Summons> findByStatusOrderByExpiresAtAsc(SummonsStatus status);

    List<Summons> findByStudentIdAndStatusOrderByExpiresAtAsc(UUID studentId, SummonsStatus status);

    List<Summons> findByStatusAndExpiresAtGreaterThanAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            SummonsStatus status, Instant from, Instant to);
}
