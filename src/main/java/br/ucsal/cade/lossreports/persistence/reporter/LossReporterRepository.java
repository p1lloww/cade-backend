package br.ucsal.cade.lossreports.persistence.reporter;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface LossReporterRepository extends JpaRepository<LossReporter, UUID> {

    /** Lê o estudante e marca a versão para subir no commit, mesmo sem alteração (RN05). Exige transação. */
    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<LossReporter> findLockedByStudentId(UUID studentId);
}
