package br.ucsal.cade.pickup.persistence.pickup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PickupRepository extends JpaRepository<Pickup, UUID> {

    /** As retiradas do estudante, da mais recente para a mais antiga. */
    List<Pickup> findByStudentIdOrderByHandedOverAtDesc(UUID studentId);

    /** Retirada de outro estudante não aparece (RF14). */
    Optional<Pickup> findByIdAndStudentId(UUID id, UUID studentId);
}
