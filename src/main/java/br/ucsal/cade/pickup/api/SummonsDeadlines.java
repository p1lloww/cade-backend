package br.ucsal.cade.pickup.api;

import java.time.Instant;
import java.util.List;

/** Os prazos das convocações, para o aviso de que a convocação vai vencer (RF34). */
public interface SummonsDeadlines {

    /**
     * As convocações Ativas que vencem depois de {@code from} e até {@code to}, as que vencem primeiro no topo. As já
     * vencidas ficam de fora: o vencimento em si é do RET-02.
     */
    List<ExpiringSummons> expiringBetween(Instant from, Instant to);
}
