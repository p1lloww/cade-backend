package br.ucsal.cade.pickup.persistence.pickup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PickupConfirmationRepository extends JpaRepository<PickupConfirmation, UUID> {

    boolean existsByPickupId(UUID pickupId);

    List<PickupConfirmation> findByPickupIdIn(Collection<UUID> pickupIds);
}
