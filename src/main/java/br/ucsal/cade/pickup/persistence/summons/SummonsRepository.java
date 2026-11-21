package br.ucsal.cade.pickup.persistence.summons;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SummonsRepository extends JpaRepository<Summons, UUID> {
}
