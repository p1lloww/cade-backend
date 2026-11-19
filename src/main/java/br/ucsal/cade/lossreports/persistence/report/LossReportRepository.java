package br.ucsal.cade.lossreports.persistence.report;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LossReportRepository extends JpaRepository<LossReport, UUID> {
}
