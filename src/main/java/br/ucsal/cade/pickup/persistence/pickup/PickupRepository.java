package br.ucsal.cade.pickup.persistence.pickup;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PickupRepository extends JpaRepository<Pickup, UUID> {
}
