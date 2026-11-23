package br.ucsal.cade.retention.persistence.disposal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DisposalRepository extends JpaRepository<Disposal, UUID> {
}
