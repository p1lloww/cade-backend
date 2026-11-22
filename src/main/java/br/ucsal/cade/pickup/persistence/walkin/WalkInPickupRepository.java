package br.ucsal.cade.pickup.persistence.walkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalkInPickupRepository extends JpaRepository<WalkInPickup, UUID> {
}
