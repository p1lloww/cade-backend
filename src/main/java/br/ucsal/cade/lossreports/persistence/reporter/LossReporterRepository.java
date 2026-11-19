package br.ucsal.cade.lossreports.persistence.reporter;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LossReporterRepository extends JpaRepository<LossReporter, UUID> {

    @Modifying
    @Query(value = "INSERT INTO loss_reporters (student_id, version) VALUES (:studentId, 0) ON CONFLICT DO NOTHING",
            nativeQuery = true)
    void createIfAbsent(UUID studentId);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    Optional<LossReporter> findLockedByStudentId(UUID studentId);
}
