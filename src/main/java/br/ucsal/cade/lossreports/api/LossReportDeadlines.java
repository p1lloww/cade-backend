package br.ucsal.cade.lossreports.api;

import java.time.Instant;
import java.util.List;

/** Os prazos dos registros de perda, para o aviso de que o registro vai vencer (RF35). */
public interface LossReportDeadlines {

    /**
     * Os registros Abertos ou Aguardando informações que vencem depois de {@code from} e até {@code to}, os que vencem
     * primeiro no topo. Convocado não entra: enquanto a convocação corre, o registro não vence (seção 6.2).
     */
    List<ExpiringLossReport> expiringBetween(Instant from, Instant to);
}
