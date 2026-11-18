package br.ucsal.cade.administration.persistence.audit;

import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório estreito do histórico. Estende {@link Repository} e não {@code JpaRepository} de propósito: só expõe
 * gravar e ler, sem nenhum método de exclusão, porque os registros do histórico nunca são apagados (RN19).
 */
public interface AuditRecordRepository extends Repository<AuditRecord, UUID> {

    <S extends AuditRecord> S save(S record);

    Optional<AuditRecord> findById(UUID id);

    List<AuditRecord> findAll();

    long count();
}
