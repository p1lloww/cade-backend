package br.ucsal.cade.lossreports.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Registros de perda em que o estudante ainda procura o objeto (Aberto ou Aguardando informações), para o cruzamento
 * (RF20).
 */
public interface MatchableLossReports {

    /** Vazio se o registro não existe ou não está mais em busca. */
    Optional<MatchableLossReport> find(UUID lossReportId);

    List<MatchableLossReport> findByCategory(UUID categoryId);
}
